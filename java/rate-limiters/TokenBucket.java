import java.util.*;

import javax.print.StreamPrintService;

public class TokenBucket{

    private long capacity;
    private double refillRatePerMs;

    private double tokens; 
    private long lastRefillTime;

    public TokenBucket(long capacity, double refillTokenPerSecond){
        if(capacity <=0){
            throw new IllegalArgumentException("capacity must be > 0");
        }

        if(refillTokenPerSecond <= 0){
            throw new IllegalArgumentException("refill rate must be > 0");
        }

        this.capacity = capacity;
        this.refillRatePerMs = refillTokenPerSecond/1000;
        
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /*
    Returns true if one request is allowed, otherwise false
    */
    public boolean allow(){
        refillBucket();

        if(tokens >= 1){
            tokens -=1;
            return true;
        }

        return false;
    }

    public void refillBucket(){
        long currentTime = System.currentTimeMillis();
        double elapsedTime = (currentTime - lastRefillTime);

        if(elapsedTime <=0){
            return;
        }

        // Calculate tokens to add: milliseconds × (tokens/millisecond) = tokens
        double tokensToAdd = elapsedTimeMs * refillRatePerMs;
        tokens = Math.min(capacity, tokens + tokensToAdd);
        lastRefillTime = currentTime;
    }
}