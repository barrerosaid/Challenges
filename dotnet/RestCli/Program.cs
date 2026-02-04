using System.Net.Http.Headers;
using System.Text;

internal sealed class Args
{
    public required string BaseUrl { get; init; }
    public required string Path { get; init; }
    public string Method { get; init; } = "GET";
    public string? Token { get; init; }
    public List<string> Query { get; } = new();
    public List<string> Header { get; } = new();
    public string? Data { get; init; }
    public string? DataFile { get; init; }
    public int TimeoutSeconds { get; init; } = 10;
    public bool Pretty { get; init; } = false;
}

public static class Program
{
    public static async Task<int> Main(string[] argv)
    {
        try
        {
            var a = ParseArgs(argv);
            await RunAsync(a);
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Error: {ex.Message}");
            PrintUsage();
            return 1;
        }
    }

    private static async Task RunAsync(Args a)
    {
        var headers = ParseHeaders(a.Header);
        var query = ParseQuery(a.Query);

        if (!string.IsNullOrWhiteSpace(a.Token) && !headers.ContainsKey("Authorization"))
        {
            headers["Authorization"] = $"Bearer {a.Token}";
        }

        string? payload = await LoadPayloadAsync(a.Data, a.DataFile);
        bool hasBody = payload is not null;

        var method = a.Method.Trim().ToUpperInvariant();
        var bodyAllowed = method is "POST" or "PUT" or "PATCH";
        if (hasBody && !bodyAllowed)
        {
            throw new ArgumentException($"Request body is only supported for POST/PUT/PATCH (got {method})");
        }

        // Default to JSON content-type when sending a body
        if (hasBody && !headers.ContainsKey("Content-Type"))
        {
            headers["Content-Type"] = "application/json";
        }

        var uri = BuildUri(a.BaseUrl, a.Path, query);

        using var http = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(a.TimeoutSeconds)
        };

        using var req = new HttpRequestMessage(new HttpMethod(method), uri);

        // Apply headers (Content-Type must go on Content, not Headers)
        foreach (var kv in headers)
        {
            if (kv.Key.Equals("Content-Type", StringComparison.OrdinalIgnoreCase))
                continue;

            req.Headers.TryAddWithoutValidation(kv.Key, kv.Value);
        }

        if (hasBody)
        {
            var contentType = headers.TryGetValue("Content-Type", out var ct) ? ct : "application/json";
            req.Content = new StringContent(payload!, Encoding.UTF8, contentType);
        }

        using var resp = await http.SendAsync(req);
        var body = await resp.Content.ReadAsStringAsync();

        Console.WriteLine($"{req.Method} {uri}");
        Console.WriteLine($"Status: {(int)resp.StatusCode}");

        if (resp.Content.Headers.ContentType is not null)
            Console.WriteLine($"Content-Type: {resp.Content.Headers.ContentType}");

        if (resp.Headers.TryGetValues("x-request-id", out var reqIds))
            Console.WriteLine($"x-request-id: {string.Join(",", reqIds)}");

        Console.WriteLine(body);
    }

    // ---------------- CLI Parsing ----------------

    private static Args ParseArgs(string[] argv)
    {
        string? baseUrl = null;
        string? path = null;
        string method = "GET";
        string? token = null;
        var query = new List<string>();
        var header = new List<string>();
        string? data = null;
        string? dataFile = null;
        int timeout = 10;
        bool pretty = false;

        for (int i = 0; i < argv.Length; i++)
        {
            var arg = argv[i];

            switch (arg)
            {
                case "--base-url":
                    baseUrl = RequireValue(argv, ref i, arg);
                    break;
                case "--path":
                    path = RequireValue(argv, ref i, arg);
                    break;
                case "--method":
                    method = RequireValue(argv, ref i, arg);
                    break;
                case "--token":
                    token = RequireValue(argv, ref i, arg);
                    break;
                case "--query":
                    query.Add(RequireValue(argv, ref i, arg));
                    break;
                case "--header":
                    header.Add(RequireValue(argv, ref i, arg));
                    break;
                case "--data":
                    data = RequireValue(argv, ref i, arg);
                    break;
                case "--data-file":
                    dataFile = RequireValue(argv, ref i, arg);
                    break;
                case "--timeout":
                    timeout = int.Parse(RequireValue(argv, ref i, arg));
                    break;
                case "--pretty":
                    pretty = true;
                    break;
                case "--help":
                case "-h":
                    PrintUsage();
                    Environment.Exit(0);
                    break;
                default:
                    throw new ArgumentException($"Unknown arg: {arg}");
            }
        }

        if (string.IsNullOrWhiteSpace(baseUrl)) throw new ArgumentException("--base-url is required");
        if (string.IsNullOrWhiteSpace(path)) throw new ArgumentException("--path is required");
        if (!string.IsNullOrEmpty(data) && !string.IsNullOrEmpty(dataFile))
            throw new ArgumentException("Use only one of --data or --data-file");

        return new Args
        {
            BaseUrl = baseUrl!,
            Path = path!,
            Method = method,
            Token = token,
            Data = data,
            DataFile = dataFile,
            TimeoutSeconds = timeout,
            Pretty = pretty
        }.Also(a =>
        {
            a.Query.AddRange(query);
            a.Header.AddRange(header);
        });
    }

    private static string RequireValue(string[] argv, ref int i, string flag)
    {
        if (i + 1 >= argv.Length) throw new ArgumentException($"Missing value for {flag}");
        var next = argv[++i];
        if (next.StartsWith("--")) throw new ArgumentException($"Missing value for {flag}");
        return next;
    }

    private static void PrintUsage()
    {
        Console.Error.WriteLine("""
Usage:
  dotnet run -- --base-url <url> --path <path> [options]

Required:
  --base-url <url>
  --path <path>

Options:
  --method <GET|POST|PUT|PATCH|DELETE>   (default: GET)
  --token <token>
  --query key=value                      (repeatable)
  --header Key:Value                     (repeatable)
  --data <json>
  --data-file <path>
  --timeout <seconds>                    (default: 10)
  --pretty
  --help
""");
    }

    // ---------------- Helpers ----------------

    private static Dictionary<string, string> ParseQuery(List<string> items)
    {
        var dict = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var s in items)
        {
            var idx = s.IndexOf('=');
            if (idx <= 0) throw new ArgumentException($"Invalid query key=value: {s}");
            dict[s[..idx]] = s[(idx + 1)..];
        }
        return dict;
    }

    private static Dictionary<string, string> ParseHeaders(List<string> items)
    {
        var dict = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var s in items)
        {
            var idx = s.IndexOf(':');
            if (idx <= 0) throw new ArgumentException($"Invalid header Key:Value: {s}");
            dict[s[..idx].Trim()] = s[(idx + 1)..].Trim();
        }
        return dict;
    }

    private static Uri BuildUri(string baseUrl, string path, Dictionary<string, string> query)
    {
        var baseTrim = baseUrl.TrimEnd('/');
        var pathTrim = path.TrimStart('/');
        var ub = new UriBuilder($"{baseTrim}/{pathTrim}");

        if (query.Count > 0)
        {
            var qs = string.Join("&", query.Select(kv =>
                $"{Uri.EscapeDataString(kv.Key)}={Uri.EscapeDataString(kv.Value)}"));
            ub.Query = qs;
        }

        return ub.Uri;
    }

    private static async Task<string?> LoadPayloadAsync(string? inline, string? file)
    {
        if (!string.IsNullOrEmpty(inline) && !string.IsNullOrEmpty(file))
            throw new ArgumentException("Use only one of --data or --data-file");

        if (!string.IsNullOrEmpty(file))
            return await File.ReadAllTextAsync(file, Encoding.UTF8);

        return string.IsNullOrEmpty(inline) ? null : inline;
    }

    // Tiny helper to mutate an init-only object after construction
    private static T Also<T>(this T obj, Action<T> f)
    {
        f(obj);
        return obj;
    }
}
