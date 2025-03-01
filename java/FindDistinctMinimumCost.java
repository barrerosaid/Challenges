import java.util.*;

public class FindDistinctMinimumCost
{
    public static void main(String[] args){
        List<Integer> sizeList = Arrays.asList(1,3,9,7,8);
        List<Integer> costList = Arrays.asList(5,2,5,7,5);
        int result = solve(sizeList, costList);
        System.out.println(result);
    }

    public static int solve(List<Integer> size, List<Integer> cost){
        int n = size.size();
        int totalCost = 0;

        // Create pairs of (size, cost) and sort by size first
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pairs.add(new int[]{size.get(i), cost.get(i)});
        }
        Collections.sort(pairs, (a, b) -> a[0] - b[0]);

        int lastUsedSize = Integer.MIN_VALUE;
        for (int[] pair : pairs) {
            int currentSize = pair[0];
            int currentCost = pair[1];
            System.out.println(currentCost + " " + currentSize);
            if (currentSize > lastUsedSize) {
                lastUsedSize = currentSize;
            } else {
                while (currentSize <= lastUsedSize) {
                    System.out.println(totalCost);
                    totalCost += currentCost;
                    currentSize++;
                }
                lastUsedSize = currentSize;
            }
        }

        return totalCost;

    }
}