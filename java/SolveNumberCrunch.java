import java.util.*;

public class SolveNumberCrunch{

    public static void main(String[] args){
        //Test0: [1,2,5,6] => 0
        System.out.println(solve(Arrays.asList(1,2,5,6)));
        //Test1: [3,3,1,1,2] =>1
        System.out.println(solve(Arrays.asList(3,3,1,1,2)));
        //Test2 [6,2,2,2,5,1,2] => 2
        System.out.println(solve(Arrays.asList(6,2,2,2,5,1,2)));
        //Test3 [4,1,2,5,6] =>1
        System.out.println(solve(Arrays.asList(4,1,2,5,6)));
    }

    public static int solve(List<Integer> list){
        if(list == null || list.size() == 0){
            return 0;
        }

        Map<Integer, Integer> numberMap = new HashMap<>();
        for(int num: list){
            if(numberMap.containsKey(num)){
                numberMap.put(num, numberMap.get(num)+1);
            } else{
                numberMap.put(num, 1);
            }
        }

        if(numberMap.size() <= 1){
            return list.size();
        }

        if(list.size() == 2){
            int[] numCount= new int[2];
            int index = 0;
            Iterator<Integer> iterator = numberMap.values().iterator();
            while(iterator.hasNext()){
                numCount[index] = iterator.next();
                index++;
            }

            return Math.abs(numCount[0]- numCount[1]);
        }

        List<Integer> numCountList = new ArrayList<>(numberMap.values());
        Collections.sort(numCountList, Collections.reverseOrder());

        int result = numCountList.get(0);
        for(int i=1; i<numCountList.size(); i++){
            result -= Math.min(result, numCountList.get(i));
        }

        return result;
    }
}