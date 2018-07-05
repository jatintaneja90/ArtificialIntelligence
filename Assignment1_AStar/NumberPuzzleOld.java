import java.util.*;

//import NumberPuzzle.NumberPuzzleRecord;

// Solving the 16-puzzle with A* using two heuristics:
// tiles-out-of-place and total-distance-to-move

public class NumberPuzzle {
    public static final int PUZZLE_WIDTH = 4;
    public static final int BLANK = 0;
    // BETTER:  false for tiles-displaced heuristic, true for Manhattan distance
    public static boolean BETTER = false;

    // You can change this representation if you prefer.
    // If you don't, be careful about keeping the tiles and the blank
    // row and column consistent.
    private int[][] tiles;  // [row][column]
    private int blank_r, blank_c;   // blank row and column
    
    public static void main(String[] args) {
        NumberPuzzle myPuzzle = readPuzzle();
        LinkedList<NumberPuzzle> solutionSteps = myPuzzle.solve(BETTER);
        printSteps(solutionSteps);
    }

    NumberPuzzle() {
        tiles = new int[PUZZLE_WIDTH][PUZZLE_WIDTH];
    }

    static NumberPuzzle readPuzzle() {
        NumberPuzzle newPuzzle = new NumberPuzzle();

        Scanner myScanner = new Scanner(System.in);
        int row = 0;
        while (myScanner.hasNextLine() && row < PUZZLE_WIDTH) {
            String line = myScanner.nextLine();
            String[] numStrings = line.split(" ");
            for (int i = 0; i < PUZZLE_WIDTH; i++) {
                if (numStrings[i].equals("-")) {
                    newPuzzle.tiles[row][i] = BLANK;
                    newPuzzle.blank_r = row;
                    newPuzzle.blank_c = i;
                } else {
                    newPuzzle.tiles[row][i] = new Integer(numStrings[i]);
                }
            }
            row++;
        }
        return newPuzzle;
    }

    public String toString() {
        String out = "";
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (j > 0) {
                    out += " ";
                }
                if (tiles[i][j] == BLANK) {
                    out += "-";
                } else {
                    out += tiles[i][j];
                }
            }
            out += "\n";
        }
        return out;
    }

    public NumberPuzzle copy() {
        NumberPuzzle clone = new NumberPuzzle();
        clone.blank_r = blank_r;
        clone.blank_c = blank_c;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                clone.tiles[i][j] = this.tiles[i][j];
            }
        }
        return clone;
    }

    // betterH:  if false, use tiles-out-of-place heuristic
    //           if true, use total-manhattan-distance heuristic
    LinkedList<NumberPuzzle> solve(boolean betterH) {
        // TODO - placeholder just to compile
    	int distanceCovered = 0;
    	int currentStateHeuristicVal = betterH ? getManhattonDistHeuristicValue(this) : getMisplacedTilesHeuristicValue(this);
    	NumberPuzzleRecord puzzleRec = new NumberPuzzleRecord(this,distanceCovered, currentStateHeuristicVal);
    	LinkedList<NumberPuzzle> stateSequence = new LinkedList<NumberPuzzle>();
//    	initialize a priority queue
    	Queue<NumberPuzzleRecord> pq = new PriorityQueue<NumberPuzzleRecord>();
    	pq.add(puzzleRec);
    	while(pq.size()>0) {
    		NumberPuzzleRecord lowestKeyRecord = pq.poll();
    		stateSequence.add(lowestKeyRecord.puzzleData);
//    		check if reach goal state return stateSequence
    		if(lowestKeyRecord.puzzleData.solved()) {
    			return stateSequence;
    		}
//    		generating neighbors and adding them to priority queue
    		distanceCovered++;
    		LinkedList<NumberPuzzle> neighborsList = getPossibleNeighborsState(lowestKeyRecord.puzzleData);
    		for(NumberPuzzle puzzle: neighborsList) {
    			currentStateHeuristicVal = betterH ? getManhattonDistHeuristicValue(puzzle) : getMisplacedTilesHeuristicValue(puzzle);
    			puzzleRec = new NumberPuzzleRecord(puzzle,distanceCovered, currentStateHeuristicVal);
    			pq.add(puzzleRec);
    		}
    	}
    	return null;
    }

    public boolean solved() {
        int shouldBe = 1;
        for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (tiles[i][j] != shouldBe) {
                    return false;
                } else {
                    // Take advantage of BLANK == 0
                    shouldBe = (shouldBe + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
                }
            }
        }
        return true;
    }

    static void printSteps(LinkedList<NumberPuzzle> steps) {
        for (NumberPuzzle s : steps) {
            System.out.println(s);
        }
    }
//    function to return possible next steps given current configuration
    LinkedList<NumberPuzzle> getPossibleNeighborsState(NumberPuzzle currentConfiguration){
    	LinkedList<NumberPuzzle> steps = new LinkedList<NumberPuzzle>();
    	if(currentConfiguration.blank_r-1 >= 0) {
    		NumberPuzzle newStep = currentConfiguration.copy();
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = currentConfiguration.tiles[currentConfiguration.blank_r-1][currentConfiguration.blank_c];
    		newStep.tiles[currentConfiguration.blank_r-1][currentConfiguration.blank_c] = NumberPuzzle.BLANK;
    		newStep.blank_r = currentConfiguration.blank_r-1;
    		steps.add(newStep);
    	}
    	if(currentConfiguration.blank_r+1 < NumberPuzzle.PUZZLE_WIDTH) {
    		NumberPuzzle newStep = currentConfiguration.copy();
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = currentConfiguration.tiles[currentConfiguration.blank_r+1][currentConfiguration.blank_c];
    		newStep.tiles[currentConfiguration.blank_r+1][currentConfiguration.blank_c] = NumberPuzzle.BLANK;
    		newStep.blank_r = currentConfiguration.blank_r+1;
        	steps.add(newStep);
    	}
    	if(currentConfiguration.blank_c-1 >= 0) {
    		NumberPuzzle newStep = currentConfiguration.copy();
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = currentConfiguration.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c-1];
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c-1] = NumberPuzzle.BLANK;
    		newStep.blank_c = currentConfiguration.blank_c-1;
        	steps.add(newStep);
    	}
    	if(currentConfiguration.blank_c+1 < NumberPuzzle.PUZZLE_WIDTH) {
    		NumberPuzzle newStep = currentConfiguration.copy();
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = currentConfiguration.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c+1];
    		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c+1] = NumberPuzzle.BLANK;
    		newStep.blank_c = currentConfiguration.blank_c+1;
        	steps.add(newStep);
    	}
    	return steps;
    }
    
//	 applying heuristic for number of tiles in wrong place
    int getMisplacedTilesHeuristicValue(NumberPuzzle puzzle){
    	int count = 0;
    	int currentNumber = 1;
    	for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
                if (puzzle.tiles[i][j] != currentNumber) {
//                	System.out.println("Incrementing count at i " + i + " j " + j + " and currentnumber is " + currentNumber);
                    count++;
                } 
                currentNumber = (currentNumber + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
            }
        }
    	return count;
    }
    
//    applying Manhatton distance as heuristic value
    int getManhattonDistHeuristicValue(NumberPuzzle puzzle) {
    	// TODO - placeholder just to compile
    	return 0;
    }
    
    public class NumberPuzzleRecord implements Comparable<NumberPuzzleRecord> {
    	NumberPuzzle puzzleData;
    	int currentDistance;
    	int heuristicValue;
    	int key;
    	
    	public NumberPuzzleRecord(NumberPuzzle puzzle, int dist, int heuristicVal) {
    		puzzleData = puzzle;
    		currentDistance = dist;
    		heuristicValue = heuristicVal;
    		key = currentDistance + heuristicValue;
    	}
    	
    	@Override
    	public int compareTo(NumberPuzzleRecord newPuzzleRecord) {
    		return Integer.compare(this.key, newPuzzleRecord.key);
    	}
    	@Override
    	public String toString() {
    		String returnStr="";
    		returnStr = returnStr + puzzleData.toString() + currentDistance + "  +  " + heuristicValue + "  =  " + key + "\n";
    		return returnStr;
    	}
    	
    }

}
