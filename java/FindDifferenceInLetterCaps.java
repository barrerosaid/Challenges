import java.utils.*;

public class FindDifferenceInLetterCaps{

    /*
     * Given a string "ABCDfghi", find the difference between upper and lower case
     * character count in the string
     * 
     * Ensure to skip any digits and only count chars a-z && A-Z
     * 
     */
    public static void main(String[] args){
        System.out.println(solve("ABCDEFghi")); //3 (6-3)
    }

    public static int solve(String str){
        char[] chArr = str.toCharArray();
        int upperCase = 0;
        int lowerCase = 0;
        for(int i=0; i<chArr.length; i++){
            char ch = chArr[i];
            if(Character.isDigit(ch)){
                continue;
            }
            if(Character.isUpperCase(ch)){
                upperCase++;
            } else{
                lowerCase++;
            }
        }

        return upperCase-lowerCase;
    }
}