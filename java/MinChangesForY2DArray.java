/*
 * Given a 2D Array composed of 1s, 2s, and 0s, determine the 
 * minimum number of changes to make this array into an Y 2D array
 * 
 * Y 2D Array Example 
 * [1, 2, 0, 1, 1]
 * [0, 1, 0, 1, 0]
 * [0, 0, 1, 2, 0]
 * [0, 0, 1, 0, 0]
 * [2, 0, 1, 2, 0]
 * 
 * Y 2D Array Correct 
 * [1, 0, 0, 0, 1]
 * [0, 1, 0, 1, 0]
 * [0, 0, 1, 0, 0]
 * [0, 0, 1, 0, 0]
 * [0, 0, 1, 0, 0]
 * 
 * Min moves: 5 moves changing the 2D array
 */

public class MinChangesForY2DArray{
    public static void main(String [] args){
        int[][] arr = {
            {1, 2, 0, 1, 1},
            {0, 1, 0, 1, 0},
            {0, 0, 1, 2, 0},
            {0, 0, 1, 0, 0},
            {2, 0, 1, 2, 0}};
        System.out.println(solve(arr));

    }

    public static int solve(int[][] arr){
        int minChanges = Integer.MAX_VALUE;
        int rows = arr.length;
        int cols = arr[0].length;
        int midYIndex = cols/2;
        int[] backgroundCells = {0, 1, 2};

        for(int y: backgroundCells){
            for(int background: backgroundCells){
                if(y == background){
                    continue;
                }
                int changes = countChanges(arr, rows, cols, midYIndex, y, background);
                minChanges = Math.min(changes, minChanges);
            }
        }

        return minChanges;
    }

    public static int countChanges(int[][] matrix, int rows, int cols, int mid, int yValue, int background){
        int changes = 0;
        for(int i=0; i<rows; i++){
            for(int j=0; j<cols; j++){
                // (j, i) are in the left top half || (j, i) in right top half || you are in the middle section
                /*
                boolean isYCell = (j == mid && i >= mid) || // Vertical part
                                  (i < mid && j == mid - i) || // Left diagonal
                                  (i < mid && j == mid + i);   // Right diagonal

                i < rows / 2 → Restricts to the upper half of the grid.
                j >= 0 && j < cols → Ensures j is within valid column bounds.
                (j == midCol - i || j == midCol + i) → Diagonal check:
                The left diagonal equation j == midCol - i
                The right diagonal equation j == midCol + i
                 
                boolean isYCell = (j == mid && i >= rows / 2) ||  // diagonals
                                    (i < rows / 2 && // upper half
                                    j >= 0 && j < cols &&  //diagonals don't go out of bound
                                    (j == mid - i || j == mid + i)); // Upper diagonals

                */
                
                boolean isYCell;
                if (i < rows / 2) {
                    // For the upper half, the Y arms: top row has Y cells at columns 0 and cols-1,
                    // second row has Y cells at columns 1 and cols-2, etc.
                    isYCell = (j == i || j == cols - i - 1);
                } else {
                    // For the lower half, the Y is the vertical stem in the center.
                    isYCell = (j == mid);
                }

                int expected = isYCell ? yValue: background;
                if(matrix[i][j] != expected){
                    //System.out.println("Changing (" + i + "," + j + ") from " + matrix[i][j] + " to " + expected);
                    changes++;
                }
            }
        }

        return changes;

    }
}