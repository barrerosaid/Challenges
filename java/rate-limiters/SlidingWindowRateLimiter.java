import java.util.*;

public class SlidingWindowRateLimiter{

    int windowSize; //capacity
    long timeLimit;

    Map<String, Deque<Long>> rateLimiter;

    public SlidingWindowRateLimiter(){
        //5 size with 10 seconds as default
        this(5, 10000L);
    }

    public SlidingWindowRateLimiter(int size, long timeLimit){
        this.windowSize = size;
        this.timeLimit = timeLimit;

        rateLimiter = new HashMap<>();
    }

    /*

    Time Complexity:

    isAllowed(): O(n) where n = number of old entries to remove
    Worst case: O(windowSize) per request

    Space Complexity: O(users × windowSize) - stores every timestamp
    
    */
    public boolean isAllowed(String user){
        long currentTime = System.currentTimeMillis();
        if(!rateLimiter.containsKey(user)){
            rateLimiter.put(user, new ArrayDeque<>());
        }
        
        Deque<Long> timesForUser = rateLimiter.get(user);

        //clean up 
        while(!timesForUser.isEmpty() && currentTime-timesForUser.peekFirst()> timeLimit){
            timesForUser.pollFirst();
        }

        if(timesForUser.size() >= windowSize){
            return false;
        }

        timesForUser.addLast(currentTime);
        return true;
    }
}