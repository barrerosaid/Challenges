public class FindSmallestString{

	/*
	 * Given a string such as "abdca", return smallest string that can be created lexographically
	 * if you were to rotate k spots from the front or back of the string
	 * 
	 * Ex 1:
	 * 1 from front: abdcaa => abdcaa (nothing changes)
	 * 2 from front: abdcaa => badcaa
	 * 3 from front: abdcaa => dbacaa
	 * 4 from front: abdcaa => cdbaaa
	 * 5 from front: abdcaa => acdbaa
	 * 6 from front: aabdca => acdbaa
	 * 
	 * 1 from back: abdcaa => acdbaa (nothing changes)
	 * 2 from back: abdcaa => acdbaa (same char, no changes)
	 * 3 from back: abdcaa => acdaab 
	 * 4 from back: abdcaa => acaabd
	 * 5 from back: abdcaa => aaabdc *
	 * 6 from back: abdcaa => aacdba 
	 * 
	 * Note: time complexity of brute force algorithm is in O(n^2)
	 * Optimization: Use a deque and lexographically sort via characters and reduce to O(n) time
	 */
	public static void main(String[] args){
		System.out.println(solve("abdcaa")); //return aaabdc
	}

	public static String solve(String s){
		String smallest = s;
		int length = s.length();
		for(int i=2; i<=length; i++){
			String prefix = s.substring(0, i);
			String prefixReverse = new StringBuilder(prefix).reverse().toString();
			String postPrefix = s.substring(i);
			String prefixWord = prefixReverse+postPrefix;
			if(prefixWord.compareTo(smallest) < 0){
				smallest = prefixWord;
			}
			
			String suffix = s.substring(length-i);
			String suffixReverse = new StringBuilder(suffix).reverse().toString();
			String preSuffix = s.substring(0, length-i);
			String suffixWord = preSuffix+suffixReverse;

			if(suffixWord.compareTo(smallest) < 0){
				smallest = suffixWord;
			}

		}

		return smallest;
	}

}

