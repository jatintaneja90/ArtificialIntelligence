import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class OnIce {

    public static final double GOLD_REWARD = 100.0;
    public static final double PIT_REWARD = -150.0;
    public static final double DISCOUNT_FACTOR = 0.5;
    public static final double EXPLORE_PROB = 0.2;  // for Q-learning
    public static final double LEARNING_RATE = 0.1;
    public static final int ITERATIONS = 10000;
    public static final int MAX_MOVES = 1000;

    // Using a fixed random seed so that the behavior is a little
    // more reproducible across runs & students
    public static Random rng = new Random(2018);

    public static void main(String[] args) {
    	File f = new File("/Users/jatintaneja/Documents/Docs/Study/AI/Assignment5_MDP_QLearning/Test3.txt");
    	try {
    		Scanner myScanner = new Scanner(f);
//    		Scanner myScanner = new Scanner(System.in);
    		Problem problem = new Problem(myScanner);
            Policy policy = problem.solve(ITERATIONS);
            if (policy == null) {
                System.err.println("No policy.  Invalid solution approach?");
            } else {
                System.out.println(policy);
            }
            if (args.length > 0 && args[0].equals("eval")) {
                System.out.println("Average utility per move: " 
                                    + tryPolicy(policy, problem));
            }
    	} catch(Exception e) {
    		System.out.println("Exception occured " + e);
    	}
    }

    public static class Problem {
        public String approach;
        public double[] moveProbs;
        public ArrayList<ArrayList<String>> map;

        // Format looks like
        // MDP    [approach to be used]
        // 0.7 0.2 0.1   [probability of going 1, 2, 3 spaces]
        // - - - - - - P - - - -   [space-delimited map rows]
        // - - G - - - - - P - -   [G is gold, P is pit]
        //
        // You can assume the maps are rectangular, although this isn't enforced
        // by this constructor.

        Problem (Scanner sc) {
            approach = sc.nextLine();
            String probsString = sc.nextLine();
            String[] probsStrings = probsString.split(" ");
            moveProbs = new double[probsStrings.length];
            for (int i = 0; i < probsStrings.length; i++) {
                try {
                    moveProbs[i] = Double.parseDouble(probsStrings[i]);
                } catch (NumberFormatException e) {
                    break;
                }
            }
            map = new ArrayList<ArrayList<String>>();
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] squares = line.split(" ");
                ArrayList<String> row = new ArrayList<String>(Arrays.asList(squares));
                map.add(row);
            }
        }

        Policy solve(int iterations) {
            if (approach.equals("MDP")) {
                MDPSolver mdp = new MDPSolver(this);
//                System.out.println("Trying to solve it via MDP");
                return mdp.solve(this, iterations);
            } else if (approach.equals("Q")) {
//            	System.out.println("Trying to solve it via Q learner");
                QLearner q = new QLearner(this);
                return q.solve(this, iterations);
            }
            return null;
        }

    }

    public static class Policy {
        public String[][] bestActions;

        public Policy(Problem prob) {
            bestActions = new String[prob.map.size()][prob.map.get(0).size()];
        }

        public String toString() {
            String out = "";
            for (int r = 0; r < bestActions.length; r++) {
                for (int c = 0; c < bestActions[0].length; c++) {
                    if (c != 0) {
                        out += " ";
                    }
                    out += bestActions[r][c];
                }
                out += "\n";
            }
            return out;
        }
    }

    // Returns the average utility per move of the policy,
    // as measured from ITERATIONS random drops of an agent onto
    // empty spaces
    public static double tryPolicy(Policy policy, Problem prob) {
        int totalUtility = 0;
        int totalMoves = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            // Random empty starting loc
            int row, col;
            do {
                row = rng.nextInt(prob.map.size());
                col = rng.nextInt(prob.map.get(0).size());
            } while (!prob.map.get(row).get(col).equals("-"));
            // Run until pit, gold, or MAX_MOVES timeout 
            // (in case policy recommends driving into wall repeatedly,
            // for example)
            for (int moves = 0; moves < MAX_MOVES; moves++) {
                totalMoves++;
                String policyRec = policy.bestActions[row][col];
                // Determine how far we go in that direction
                int displacement = 1;
                double totalProb = 0;
                double moveSample = rng.nextDouble();
                for (int p = 0; p <= prob.moveProbs.length; p++) {
                    totalProb += prob.moveProbs[p];
                    if (moveSample <= totalProb) {
                        displacement = p+1;
                        break;
                    }
                }
                int new_row = row;
                int new_col = col;
                if (policyRec.equals("U")) {
                    new_row -= displacement;
                    if (new_row < 0) {
                        new_row = 0;
                    }
                } else if (policyRec.equals("R")) {
                    new_col += displacement;
                    if (new_col >= prob.map.get(0).size()) {
                        new_col = prob.map.get(0).size()-1;
                    }
                } else if (policyRec.equals("D")) {
                    new_row += displacement;
                    if (new_row >= prob.map.size()) {
                        new_row = prob.map.size()-1;
                    }
                } else if (policyRec.equals("L")) {
                    new_col -= displacement;
                    if (new_col < 0) {
                        new_col = 0;
                    }
                }
                row = new_row;
                col = new_col;
                if (prob.map.get(row).get(col).equals("G")) {
                    totalUtility += GOLD_REWARD;
                    // End the current trial
                    break;
                } else if (prob.map.get(row).get(col).equals("P")) {
                    totalUtility += PIT_REWARD;
                    break;
                }
            }
        }

        return totalUtility/(double)totalMoves;
    }



    public static class MDPSolver {
    
        // We'll want easy access to the real rewards while iterating, so
        // we'll keep both of these around
        public double[][] utilities;
        public double[][] rewards;
        public MDPSolver(Problem prob) {
            utilities = new double[prob.map.size()][prob.map.get(0).size()];
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            // Initialize utilities to the rewards in their spaces,
            // else 0
            for (int r = 0; r < utilities.length; r++) {
                for (int c = 0; c < utilities[0].length; c++) {
                    String spaceContents = prob.map.get(r).get(c);
                    if (spaceContents.equals("G")) {
                        utilities[r][c] = GOLD_REWARD;
                        rewards[r][c] = GOLD_REWARD;
                    } else if (spaceContents.equals("P")) {
                        utilities[r][c] = PIT_REWARD;
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        utilities[r][c] = 0.0;
                        rewards[r][c] = 0.0;
                    }
                }
            }
        }

        Policy solve(Problem prob, int iterations) {
            Policy policy = new Policy(prob);
            // TODO your code here & you'll probably want at least one helper function
            final String[] actions = {"U", "R", "D", "L"};
            double[] moveProbs = prob.moveProbs;
            
            for(int iteration=0; iteration < iterations; iteration++) {
            	for(int i=0; i<utilities.length;i++) {
            		for(int j=0;j<utilities[0].length; j++) {
            			String bestDir="";
            			double maxUtility = Integer.MIN_VALUE;
//            			double currentIterationUtility = 0.0;
            			for(int dir=0;dir < actions.length; dir++) {
            				double currentDirectionUtility = 0.0;
            				if(actions[dir].equals("U") && i!=0) {
            					for(int k=1;k<=moveProbs.length;k++) {
            						int new_i = i-k;
            						if(new_i < 0)
            							new_i = 0;
            						currentDirectionUtility += moveProbs[k-1] * utilities[new_i][j];
            					}
            				} else if(actions[dir].equals("R") && j != utilities[0].length -1) {
            					for(int k=1;k<=moveProbs.length;k++) {
            						int new_j = j+k;
            						if(new_j >= utilities[0].length)
            							new_j = utilities[0].length -1;
            						currentDirectionUtility += moveProbs[k-1] * utilities[i][new_j];
            					}
            				} else if(actions[dir].equals("D") && i != utilities.length -1) {
            					for(int k=1;k<=moveProbs.length;k++) {
            						int new_i = i+k;
            						if(new_i >= utilities.length)
            							new_i = utilities.length-1;
            						currentDirectionUtility += moveProbs[k-1] * utilities[new_i][j];
            					}
            				} else if(actions[dir].equals("L") && j!=0) {
            					for(int k=1;k<=moveProbs.length;k++) {
            						int new_j = j-k;
            						if(new_j < 0)
            							new_j = 0;
            						currentDirectionUtility += moveProbs[k-1] * utilities[i][new_j];
            					}
            				}
            				if(currentDirectionUtility > maxUtility) {
            					maxUtility = currentDirectionUtility;
            					bestDir = actions[dir];
            				}
            			}
            			utilities[i][j] = rewards[i][j] + (DISCOUNT_FACTOR *  maxUtility);
            			if(rewards[i][j] == GOLD_REWARD)
            				policy.bestActions[i][j] = prob.map.get(i).get(j);
            			else if(rewards[i][j] == PIT_REWARD)
            				policy.bestActions[i][j] = prob.map.get(i).get(j);
            			else
            				policy.bestActions[i][j] = bestDir;
            		}
            	}
            }
//            for(int i=0; i<utilities.length;i++) {
//        		for(int j=0;j<utilities[0].length; j++) {
//        			oldUtilities[i][j] = utilities[i][j];
//        		}
//            }
            
            return policy;
        }

    }

    // QLearner:  Same problem as MDP, but the agent doesn't know what the
    // world looks like, or what its actions do.  It can learn the utilities of
    // taking actions in particular states through experimentation, but it
    // has no way of realizing what the general action model is 
    // (like "Right" increasing the column number in general).
    public static class QLearner {

        // Use these to index into the first index of utilities[][][]
        public static final int UP = 0;
        public static final int RIGHT = 1;
        public static final int DOWN = 2;
        public static final int LEFT = 3;
        public static final int ACTIONS = 4;

        public double utilities[][][];  // utilities of actions
        public double rewards[][];
        
        public QLearner(Problem prob) {
            utilities = new double[ACTIONS][prob.map.size()][prob.map.get(0).size()];
            // Rewards are for convenience of lookup; the learner doesn't
            // actually "know" they're there until encountering them
            rewards = new double[prob.map.size()][prob.map.get(0).size()];
            for (int r = 0; r < rewards.length; r++) {
                for (int c = 0; c < rewards[0].length; c++) {
                    String locType = prob.map.get(r).get(c);
                    if (locType.equals("G")) {
                        rewards[r][c] = GOLD_REWARD;
                    } else if (locType.equals("P")) {
                        rewards[r][c] = PIT_REWARD;
                    } else {
                        rewards[r][c] = 0.0; // not strictly necessary to init
                    }
                }
            }
            // Java: default init utilities to 0
        }

        public Policy solve(Problem prob, int iterations) {
            Policy policy = new Policy(prob);
            // TODO: your code here; probably wants at least one helper too
            double[] moveProbs = prob.moveProbs;
            final String[] actions = {"U", "R", "D", "L"};
            HashMap<String, Integer> actionMap = new HashMap<String, Integer>();
            int actionNumber=0;
            for(String str:actions) {
            	actionMap.put(str, actionNumber);
            	actionNumber++;
            }
            
            for (int r = 0; r < rewards.length; r++) {
                for (int c = 0; c < rewards[0].length; c++) {
                	for(int a = 0; a < ACTIONS ;a++) {
//                		if( (r==0 && a == 0) || 
//                			(c == utilities[0].length -1 && a == 1) ||
//                			(r == utilities.length -1 && a == 2) || 
//                			(c==0 && a == 3))
//                			utilities[a][r][c] = Integer.MIN_VALUE;
//                		else
                			utilities[a][r][c] = rewards[r][c];
                	}
                }
            }
            
            for(int iteration=0;iteration<iterations; iteration++) {
            	System.out.println("In Iteration: " + iteration);
//            	get a random coordinate to start exploitation/exploration
            	int row, col;
                row = rng.nextInt(prob.map.size());
                col = rng.nextInt(prob.map.get(0).size());
                while(true) {
//                	while current coordinate is not a pit/gold play and 
//                	calculate utility of current blank and move in maximum utility direction
//                	else break;
                	if(prob.map.get(row).get(col).equals("G")){
//                		needs to adjust neighbors values later
                		for(int action=0; action < ACTIONS; action++) {
                			utilities[action][row][col] = rewards[row][col];
                		}
                		policy.bestActions[row][col] = prob.map.get(row).get(col);
                		System.out.println("Found Gold at row " + row + "and col "+ col + " exiting");
                		break;
                	}
                	if(prob.map.get(row).get(col).equals("P")) {
//                		needs to adjust neighbors values later
                		for(int action=0; action < ACTIONS; action++) {
                			utilities[action][row][col] = rewards[row][col];
                		}
                		policy.bestActions[row][col] = prob.map.get(row).get(col);
//                		System.out.println("Found Pit, going to exit");
                		break;
                	}
                	double actionMax = Integer.MIN_VALUE;
                	int actionNumbr = -1;
                	for(int action=0 ; action < ACTIONS ; action++) {
//                		System.out.println("Working on action " + action + " for row " + row + " for col " + col);
                		Double currentDirectionUtility = null;
                		int displacement = 1;
                        double totalProb = 0;
                        double moveSample = rng.nextDouble();
                		for (int p = 0; p <= prob.moveProbs.length; p++) {
                            totalProb += prob.moveProbs[p];
                            if (moveSample <= totalProb) {
                            	displacement = p+1;
                                break;
                            }
                        }
                		System.out.println("current displacement is " + displacement);
                		String bestNextStateAction = null;
                		int next_row = -1,next_col = -1;
                		if(action == 0 && row!=0) {
                			System.out.println("Working on action " + action + " for row " + row + " for col " + col);
                			next_row = row-displacement;
                			next_col = col;
    						if(next_row < 0)
    							next_row = 0;
    						
//    						bestNextStateAction = policy.bestActions[next_row][next_col];
    						
                		} else if(action == 1 && col != rewards[0].length -1) {
                			System.out.println("Working on action " + action + " for row " + row + " for col " + col);
    						next_col = col+displacement;
    						next_row = row;
    						if(next_col >= rewards[0].length)
    							next_col = rewards[0].length -1;
//    						bestNextStateAction = policy.bestActions[next_row][next_col];
//            						System.out.println("bestNextStateAction " + bestNextStateAction);
//    						double bestNextStateUtility = bestNextStateAction != null ? utilities[actionMap.get(bestNextStateAction)][row][new_col] 
//    								: utilities[action][row][new_col];
//            						System.out.println("bestNextStateUtility " + bestNextStateUtility);
//    						currentDirectionUtility = bestNextStateUtility;
                		} else if(action == 2 && row != rewards.length -1) {
                			System.out.println("Working on action " + action + " for row " + row + " for col " + col);
    						next_row = row+displacement;
    						next_col = col;
    						if(next_row >= utilities.length)
    							next_row = utilities.length-1;
//    						bestNextStateAction = policy.bestActions[next_row][next_col];
//            						System.out.println("bestNextStateAction " + bestNextStateAction);
//    						double bestNextStateUtility = bestNextStateAction != null ? utilities[actionMap.get(bestNextStateAction)][new_row][col] 
//    								: utilities[action][new_row][col];
//            						System.out.println("bestNextStateUtility " + bestNextStateUtility);
//    						currentDirectionUtility = bestNextStateUtility;	
                		} else if(action == 3 && col!=0) {
                			System.out.println("Working on action " + action + " for row " + row + " for col " + col);
    						next_col = col-displacement;
    						next_row = row;
    						if(next_col < 0)
    							next_col = 0;
//    						bestNextStateAction = policy.bestActions[next_row][next_col];
//            						System.out.println("bestNextStateAction " + bestNextStateAction);
//    						double bestNextStateUtility = bestNextStateAction != null ? utilities[actionMap.get(bestNextStateAction)][row][new_col] 
//    								: utilities[action][row][new_col];
//            						System.out.println("bestNextStateUtility " + bestNextStateUtility);
//    						currentDirectionUtility = bestNextStateUtility;
                		}
                		System.out.println("bestNextStateAction is " + bestNextStateAction);
                		double bestNextStateUtility;
                		if(next_row == -1 && next_col == -1) {
                			System.out.println("Edge case found, action " + action + " for row " + row + " for col " + col);
                			continue;
                		}
                		
                		bestNextStateAction = policy.bestActions[next_row][next_col];
						if(bestNextStateAction == null) {
							bestNextStateUtility = rewards[next_row][next_col];
						} else if(bestNextStateAction.equals("G") || bestNextStateAction.equals("P"))
							bestNextStateUtility = utilities[action][next_row][next_col];
						else
							bestNextStateUtility = utilities[actionMap.get(bestNextStateAction)][next_row][next_col];
						currentDirectionUtility = bestNextStateUtility;
						System.out.println("bestNextStateUtility " + bestNextStateUtility);
                		System.out.println("currentDirectionUtility for action " + actions[action] + " is " + currentDirectionUtility);
                		utilities[action][row][col] = currentDirectionUtility == null  ? utilities[action][row][col] 
                																	: utilities[action][row][col] + (LEARNING_RATE * (rewards[row][col] 
                																		+ (DISCOUNT_FACTOR * currentDirectionUtility) 
																						- utilities[action][row][col]));
//                		utilities[action][row][col] = utilities[action][row][col] + (LEARNING_RATE * (rewards[row][col] 
//														+ (DISCOUNT_FACTOR * currentDirectionUtility) 
//														- utilities[action][row][col]));
                		System.out.println("utility for current row and col is " + utilities[action][row][col]);
                		if(utilities[action][row][col] > actionMax ) {
                			System.out.println("Updating max Utility value for row " + row + " col "  + col + " value is " + utilities[action][row][col] + 
                					" best action : " + actions[action] + " old value was " + actionMax);
                			actionMax = utilities[action][row][col];
                			actionNumbr = action;
                		}
                	}
                	policy.bestActions[row][col] = actions[actionNumbr];
//                	System.out.println("best action is :" + policy.bestActions[row][col]);
//                	at the end of current while loop
//                	either update row and col by current max utility or opt for exploration
//                	choose between exploitation and exploration
                	System.out.println("Finished working on state row : " + row + " and col : " + col);
                	int new_row = row, new_col = col;
                	if(rng.nextDouble() < EXPLORE_PROB) {
//                		random direction from current location
                		System.out.println("Choosing exploration");
                		new_row = rng.nextInt(prob.map.size());
                		new_col = rng.nextInt(prob.map.get(0).size());
                		
                	} else {
                		System.out.println("Choosing exploitation");
//                		direction guided by max utility of current
                		if(policy.bestActions[row][col].equals(actions[0])) {
                			new_row = row-1;
                			if(new_row < 0)
                				new_row = 0;
                		} else if(policy.bestActions[row][col].equals(actions[1])) {
                			new_col = col+1;
                			if(new_col >= rewards[0].length)
                				new_col = rewards[0].length -1;
                		} else if(policy.bestActions[row][col].equals(actions[2])) {
                			new_row = row + 1;
                			if(new_row >= rewards.length)
                				new_row = rewards.length -1;
                		} else if(policy.bestActions[row][col].equals(actions[3])) {
                			new_col = col-1;
                			if(new_col < 0)
                				new_col = 0;
                		}
                		row = new_row;
                		col = new_col;
                		System.out.println("New values assigned are row : " + row + " and col : " + col);
                	}
                }

            }
            return policy;
        }
    }
}
