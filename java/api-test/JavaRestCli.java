import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * JavaRestCli is a lightweight, dependency-free command-line tool for invoking REST APIs.
 *
 * Design goals:
 * - Minimal dependencies (stdlib only)
 * - Explicit, readable control flow (interview-friendly)
 * - Suitable for debugging and stepping through with a debugger
 *
 * Responsibilities:
 * - Parse command-line arguments
 * - Construct an HTTP request (method, headers, query params, optional body)
 * - Execute the request using Java's built-in HttpClient
 * - Print a human-readable summary of the response
 *
 * This class intentionally avoids frameworks or heavy abstractions to keep
 * behavior transparent and easy to reason about during pair programming.
 */
public class JavaRestCli {

    /**
     * Args is a simple data holder representing parsed command-line options.
     *
     * Defaults are chosen to match common CLI conventions:
     * - HTTP method defaults to GET
     * - Timeout defaults to 10 seconds
     *
     * Fields are mutable by design to simplify parsing logic and reduce boilerplate.
     */
    private static final class Args {
        String baseUrl;                 // Base URL (e.g. https://api.example.com)
        String path;                    // Request path (e.g. /v1/items)
        String method = "GET";          // HTTP method (default GET)
        String token;                   // Optional bearer token
        List<String> query = new ArrayList<>();   // key=value query params
        List<String> header = new ArrayList<>();  // Key:Value headers
        String data;                    // Inline JSON payload
        Path dataFile;                  // JSON payload from file
        int timeoutSeconds = 10;        // Request timeout
        boolean pretty = false;         // Reserved for future JSON pretty-printing
    }

    /**
     * Program entry point.
     *
     * Parses arguments, executes the request, and exits with a non-zero
     * status code on error.
     */
    public static void main(String[] args) {
        try {
            Args a = parseArgs(args);
            run(a);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    /**
     * Executes a single HTTP request based on parsed CLI arguments.
     *
     * Flow:
     * 1. Normalize headers and query parameters
     * 2. Load optional request body
     * 3. Build URI and HttpRequest
     * 4. Execute request synchronously
     * 5. Print response summary and body
     */
    private static void run(Args a) throws IOException, InterruptedException {
        Map<String, String> headers = parseHeaders(a.header);
        Map<String, String> params = parseQuery(a.query);

        // Attach Authorization header if token is provided
        if (a.token != null && !headers.containsKey("Authorization")) {
            headers.put("Authorization", "Bearer " + a.token);
        }

        String payload = loadPayload(a.data, a.dataFile);
        boolean hasBody = payload != null;

        // Default to JSON content-type when sending a body
        if (hasBody && !headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        URI uri = URI.create(joinUrl(a.baseUrl, a.path) + buildQueryString(params));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(a.timeoutSeconds))
                .build();

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(a.timeoutSeconds));

        headers.forEach(rb::header);

        // Apply HTTP method and optional body
        String m = a.method.toUpperCase(Locale.ROOT);
        if (hasBody) {
            rb.method(m, HttpRequest.BodyPublishers.ofString(payload));
        } else {
            rb.method(m, HttpRequest.BodyPublishers.noBody());
        }

        HttpRequest req = rb.build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        // Basic, readable output suitable for debugging
        System.out.println(req.method() + " " + uri);
        System.out.println("Status: " + resp.statusCode());
        resp.headers().firstValue("content-type").ifPresent(v -> System.out.println("Content-Type: " + v));
        resp.headers().firstValue("x-request-id").ifPresent(v -> System.out.println("x-request-id: " + v));
        System.out.println(resp.body());
    }

    /**
     * Parses command-line arguments into an Args object.
     *
     * Supported flags:
     * --base-url <url>   (required)
     * --path <path>      (required)
     * --method <HTTP method> (default GET)
     * --token <bearer token>
     * --query key=value  (repeatable)
     * --header Key:Value (repeatable)
     * --data <json>
     * --data-file <path>
     * --timeout <seconds>
     * --pretty
     */
    private static Args parseArgs(String[] argv) {
        Args a = new Args();

        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];

            switch (arg) {
                case "--base-url" -> a.baseUrl = requireValue(argv, ++i, "--base-url");
                case "--path" -> a.path = requireValue(argv, ++i, "--path");
                case "--method" -> a.method = requireValue(argv, ++i, "--method");
                case "--token" -> a.token = requireValue(argv, ++i, "--token");
                case "--query" -> a.query.add(requireValue(argv, ++i, "--query"));
                case "--header" -> a.header.add(requireValue(argv, ++i, "--header"));
                case "--data" -> a.data = requireValue(argv, ++i, "--data");
                case "--data-file" -> a.dataFile = Path.of(requireValue(argv, ++i, "--data-file"));
                case "--timeout" -> a.timeoutSeconds = Integer.parseInt(requireValue(argv, ++i, "--timeout"));
                case "--pretty" -> a.pretty = true;
                case "--help", "-h" -> {
                    printUsage();
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("Unknown arg: " + arg);
            }
        }

        if (a.baseUrl == null || a.baseUrl.isBlank())
            throw new IllegalArgumentException("--base-url is required");
        if (a.path == null || a.path.isBlank())
            throw new IllegalArgumentException("--path is required");
        if (a.data != null && a.dataFile != null)
            throw new IllegalArgumentException("Use only one of --data or --data-file");

        return a;
    }

    /**
     * Ensures that a flag is followed by a value.
     */
    private static String requireValue(String[] argv, int idx, String flag) {
        if (idx >= argv.length)
            throw new IllegalArgumentException("Missing value for " + flag);
        String v = argv[idx];
        if (v.startsWith("--"))
            throw new IllegalArgumentException("Missing value for " + flag);
        return v;
    }

    /**
     * Prints CLI usage information.
     */
    private static void printUsage() {
        System.err.println("""
                Usage:
                  java JavaRestCli --base-url <url> --path <path> [options]
                """);
    }

    /**
     * Loads request payload from inline JSON or file.
     */
    private static String loadPayload(String inline, Path file) throws IOException {
        if (inline != null && file != null)
            throw new IllegalArgumentException("Use only one of --data or --data-file");
        if (file != null)
            return Files.readString(file, StandardCharsets.UTF_8);
        return inline;
    }

    /**
     * Parses query parameters from key=value format.
     */
    private static Map<String, String> parseQuery(List<String> items) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String s : items) {
            int i = s.indexOf('=');
            if (i <= 0)
                throw new IllegalArgumentException("Invalid query key=value: " + s);
            out.put(s.substring(0, i), s.substring(i + 1));
        }
        return out;
    }

    /**
     * Parses headers from Key:Value format.
     */
    private static Map<String, String> parseHeaders(List<String> items) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String s : items) {
            int i = s.indexOf(':');
            if (i <= 0)
                throw new IllegalArgumentException("Invalid header Key:Value: " + s);
            out.put(s.substring(0, i).trim(), s.substring(i + 1).trim());
        }
        return out;
    }

    /**
     * Safely joins base URL and path.
     */
    private static String joinUrl(String baseUrl, String path) {
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    /**
     * Builds a URL-encoded query string from parameters.
     */
    private static String buildQueryString(Map<String, String> params) {
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
