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
    	NumberPuzzle goalState = generateGoalState();
    	int distanceCovered = 0;
    	int currentStateHeuristicVal = betterH ? getManhattonDistHeuristicValue(this) : getMisplacedTilesHeuristicValue(this);
    	NumberPuzzleRecord puzzleRec = new NumberPuzzleRecord(this, distanceCovered, currentStateHeuristicVal);
    	LinkedList<NumberPuzzle> stateSequence = new LinkedList<NumberPuzzle>();
//    	initialize a priority queue
    	Queue<NumberPuzzleRecord> pq = new PriorityQueue<NumberPuzzleRecord>();
    	pq.add(puzzleRec);
    	while(pq.size()>0) {
    		NumberPuzzleRecord lowestKeyRecord = pq.poll();
//    		System.out.println(lowestKeyRecord);
    		stateSequence.add(lowestKeyRecord.puzzleData);
//    		check if reach goal state return stateSequence
    		if(lowestKeyRecord.puzzleData.solved()) {
    			return stateSequence;
    		}
//    		generating neighbors and adding them to priority queue
//    		distanceCovered++;
    		LinkedList<NumberPuzzleRecord> neighborsRecordList = getPossibleNeighborsState(lowestKeyRecord, betterH, goalState);
//    		LinkedList<NumberPuzzle> neighborsList = getPossibleNeighborsState(lowestKeyRecord.puzzleData);
    		for(NumberPuzzleRecord puzzleRecord: neighborsRecordList) {
//    			currentStateHeuristicVal = betterH ? getManhattonDistHeuristicValue(puzzle) : getMisplacedTilesHeuristicValue(puzzle);
//    			puzzleRec = new NumberPuzzleRecord(puzzle,distanceCovered, currentStateHeuristicVal);
    			pq.add(puzzleRecord);
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
    LinkedList<NumberPuzzleRecord> getPossibleNeighborsState(NumberPuzzleRecord currentConfigurationRecord, Boolean betterH,
    		NumberPuzzle goalState){
    	LinkedList<NumberPuzzleRecord> steps = new LinkedList<NumberPuzzleRecord>();
    	if(!betterH) {
    		NumberPuzzle currentConfiguration = currentConfigurationRecord.puzzleData;
    		int parentHeuristicValue = currentConfigurationRecord.heuristicValue;
    		int currentDistance = currentConfigurationRecord.currentDistance + 1;
    		if(currentConfiguration.blank_r-1 >= 0) {
        		NumberPuzzle newStep = currentConfiguration.copy();
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = 
        				currentConfiguration.tiles[currentConfiguration.blank_r-1][currentConfiguration.blank_c];
        		newStep.tiles[currentConfiguration.blank_r-1][currentConfiguration.blank_c] = NumberPuzzle.BLANK;
        		newStep.blank_r = currentConfiguration.blank_r-1;
        		int childHeuristicValue = calculateNewHeuristicValue(currentConfiguration, newStep, goalState, parentHeuristicValue);
        		NumberPuzzleRecord newStepRecord = new NumberPuzzleRecord(newStep,currentDistance, childHeuristicValue);
        		steps.add(newStepRecord);
        	}
        	if(currentConfiguration.blank_r+1 < NumberPuzzle.PUZZLE_WIDTH) {
        		NumberPuzzle newStep = currentConfiguration.copy();
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = 
        				currentConfiguration.tiles[currentConfiguration.blank_r+1][currentConfiguration.blank_c];
        		newStep.tiles[currentConfiguration.blank_r+1][currentConfiguration.blank_c] = NumberPuzzle.BLANK;
        		newStep.blank_r = currentConfiguration.blank_r+1;
        		int childHeuristicValue = calculateNewHeuristicValue(currentConfiguration, newStep, goalState, parentHeuristicValue);
        		NumberPuzzleRecord newStepRecord = new NumberPuzzleRecord(newStep,currentDistance, childHeuristicValue);
            	steps.add(newStepRecord);
        	}
        	if(currentConfiguration.blank_c-1 >= 0) {
        		NumberPuzzle newStep = currentConfiguration.copy();
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = 
        				currentConfiguration.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c-1];
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c-1] = NumberPuzzle.BLANK;
        		newStep.blank_c = currentConfiguration.blank_c-1;
        		int childHeuristicValue = calculateNewHeuristicValue(currentConfiguration, newStep, goalState, parentHeuristicValue);
        		NumberPuzzleRecord newStepRecord = new NumberPuzzleRecord(newStep,currentDistance, childHeuristicValue);
            	steps.add(newStepRecord);
        	}
        	if(currentConfiguration.blank_c+1 < NumberPuzzle.PUZZLE_WIDTH) {
        		NumberPuzzle newStep = currentConfiguration.copy();
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c] = 
        				currentConfiguration.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c+1];
        		newStep.tiles[currentConfiguration.blank_r][currentConfiguration.blank_c+1] = NumberPuzzle.BLANK;
        		newStep.blank_c = currentConfiguration.blank_c+1;
        		int childHeuristicValue = calculateNewHeuristicValue(currentConfiguration, newStep, goalState, parentHeuristicValue);
        		NumberPuzzleRecord newStepRecord = new NumberPuzzleRecord(newStep,currentDistance, childHeuristicValue);
            	steps.add(newStepRecord);
        	}
    	}
    	return steps;
    }
    
//    trying to calculate heuristic of child state using parent's heuristic value.
    int calculateNewHeuristicValue(NumberPuzzle parentPuzzle,NumberPuzzle childPuzzle,NumberPuzzle goalState, int parentHeuristicValue) {
//    	int childHeuristicValue;
    	int pBlankr = parentPuzzle.blank_r;
    	int pBlankc = parentPuzzle.blank_c;
    	int cBlankr = childPuzzle.blank_r;
    	int cBlankc = childPuzzle.blank_c;
// if parent blank slot was right
    	if(parentPuzzle.tiles[pBlankr][pBlankc] == goalState.tiles[pBlankr][pBlankc]) {
    		if(childPuzzle.tiles[pBlankr][pBlankc] != goalState.tiles[pBlankr][pBlankc])
    			parentHeuristicValue += 1;
//    		if child is right then no change
    	} else {
//    		if parent's blank slot were wrong and child's parent blank slot is right
    		if(childPuzzle.tiles[pBlankr][pBlankc] == goalState.tiles[pBlankr][pBlankc])
    			parentHeuristicValue -= 1;
//    		if child is also wrong then no change
    	}
    	
    	// if child blank slot was right at parent state
    	if(parentPuzzle.tiles[cBlankr][cBlankc] == goalState.tiles[cBlankr][cBlankc]) {
    		if(childPuzzle.tiles[cBlankr][cBlankc] != goalState.tiles[cBlankr][cBlankc])
    			parentHeuristicValue += 1;
//    		if child slot is right then no change
    	} else {
//    		if parent's child blank slot were wrong and child's blank slot is right
    		if(childPuzzle.tiles[cBlankr][cBlankc] == goalState.tiles[cBlankr][cBlankc])
    			parentHeuristicValue -= 1;
//    		if child is also wrong then no change
    	}
    	
//    	if((parentPuzzle.tiles[parentPuzzle.blank_r][parentPuzzle.blank_c] == goalState.tiles[parentPuzzle.blank_r][parentPuzzle.blank_c]) &&
//    			(childPuzzle.tiles[childPuzzle.blank_r][childPuzzle.blank_c] != goalState.tiles[childPuzzle.blank_r][childPuzzle.blank_c]))
//    		childHeuristicValue = parentHeuristicValue - 1;
//    	else if((parentPuzzle.tiles[parentPuzzle.blank_r][parentPuzzle.blank_c] != goalState.tiles[parentPuzzle.blank_r][parentPuzzle.blank_c]) &&
//    			(childPuzzle.tiles[childPuzzle.blank_r][childPuzzle.blank_c] == goalState.tiles[childPuzzle.blank_r][childPuzzle.blank_c]))
//    		childHeuristicValue = parentHeuristicValue + 1;
//    	else {
////    		no change in heuristic value if for both beforeChange or afterChange values were wrong or right
//    		childHeuristicValue = parentHeuristicValue;
//    	}
    	
    	return parentHeuristicValue;
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
    
    public NumberPuzzle generateGoalState() {
    	NumberPuzzle puzzle = new NumberPuzzle();
    	int currentNumber = 1;
    	for (int i = 0; i < PUZZLE_WIDTH; i++) {
            for (int j = 0; j < PUZZLE_WIDTH; j++) {
            	puzzle.tiles[i][j] = currentNumber;
                currentNumber = (currentNumber + 1) % (PUZZLE_WIDTH*PUZZLE_WIDTH);
            }
        }
    	return puzzle;
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
