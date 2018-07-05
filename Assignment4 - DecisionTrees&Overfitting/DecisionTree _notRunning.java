import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
//import java.util.LinkedList;

// An assignment on decision trees, using the "Adult" dataset from
// the UCI Machine Learning Repository.  The dataset predicts
// whether someone makes over $50K a year from their census data.

public class DecisionTree {

    public Feature feature;   // if true, follow the yes branch
    public boolean decision;  // for leaves
    public DecisionTree yesBranch;
    public DecisionTree noBranch;

    public static double CHI_THRESH = 3.84;
    public static double EPSILON = 0.00000001;
//    public static boolean PRUNE = true;
    public static boolean PRUNE = false;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Keep header line around for interpreting decision trees
        String header = scanner.nextLine();
        Feature.featureNames = header.split(",");
        System.err.println("Reading training examples...");
        ArrayList<Example> trainExamples = readExamples(scanner);
        // We'll assume a delimiter of "---" separates train and test as before
        DecisionTree tree = new DecisionTree(trainExamples);
        System.out.println(tree);
        System.out.println("Training data results: ");
        System.out.println(tree.classify(trainExamples));
        System.err.println("Reading test examples...");
        ArrayList<Example> testExamples = readExamples(scanner);
//        System.out.println("testExamples: " + testExamples);
//        System.err.println("Going to classify test examples...");
        Results results = tree.classify(testExamples);
        System.out.println("Test data results: ");
        System.out.print(results);
    }

    public static ArrayList<Example> readExamples(Scanner scanner) {
        ArrayList<Example> examples = new ArrayList<Example>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
//            System.out.println(line);
            if (line.startsWith("---")) {
                break;
            }
            // Skip missing data lines
            if (!line.contains("?")) {
                Example newExample = new Example(line);
                examples.add(newExample);
            }
        }
//        System.out.println("returning examples");
        return examples;
    }

    public static class Example {
        public String[] strings;     // Use only if isNumerical[i] is false
        public double[] numericals;  // Use only if isNumerical[i] is true
        boolean target;

        public Example(String dataline) {
            // Assume a basic CSV with no double-quotes to handle real commas
            strings = dataline.split(",");
            // We'll maintain a separate array with everything that we can
            // put into numerical form, in numerical form.
            // No real need to distinguish doubles from ints.
            numericals = new double[strings.length];
            if (Feature.isNumerical == null) {
                // First data line; we're determining types
                Feature.isNumerical = new boolean[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    if (Feature.featureNames[i].equals("Target")) {
                        target = strings[i].equals("1");
                    } else {
                        try {
                            numericals[i] = Double.parseDouble(strings[i]);
                            Feature.isNumerical[i] = true;
                        } catch (NumberFormatException e) {
                            Feature.isNumerical[i] = false;
                            // string stays where it is, in strings
                        }
                    }
                }
            } else {
                for (int i = 0; i < strings.length; i++) {
                    if (i >= Feature.isNumerical.length) {
                        System.err.println("Too long line: " + dataline);
                    } else if (Feature.featureNames[i].equals("Target")) {
                        target = strings[i].equals("1");
                    } else if (Feature.isNumerical[i]) {
                        try {
                            numericals[i] = Double.parseDouble(strings[i]);
                        } catch (NumberFormatException e) {
                            Feature.isNumerical[i] = false;
                            // string stays where it is
                        }
                    }
                }
            }
        }

        public String toString() {
            String out = "";
            for (int i = 0; i < Feature.featureNames.length; i++) {
                out += Feature.featureNames[i] + "=" + strings[i] + ";";
            }
            return out;
        }
    }

    public static class Feature {
        public int featureNum;
        // WLOG assume numerical features are "less than"
        // and String features are "equal to"
        public String svalue;
        public double dvalue;
        public static String[] featureNames;
        public static boolean[] isNumerical = null;

        public Feature(int featureNum, String value) {
            this.featureNum = featureNum;
            this.svalue = value;
        }

        public Feature(int featureNum, double value) {
            this.featureNum = featureNum;
            this.dvalue = value;
        }

        public boolean apply(Example e) {
            if (Feature.isNumerical[featureNum]) {
                return (e.numericals[featureNum] < dvalue);
            } else {
                return (e.strings[featureNum].equals(svalue));
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof Feature)) {
                return false;
            }
            Feature otherFeature = (Feature) o;
            if (featureNum != otherFeature.featureNum) {
                return false;
            } else if (Feature.isNumerical[featureNum]) {
                if (Math.abs(dvalue - otherFeature.dvalue) < EPSILON) {
                    return true;
                }
                return false;
            } else {
                if (svalue.equals(otherFeature.svalue)) {
                    return true;
                }
                return false;
            }
        }
        
        public int hashCode() {
            return (featureNum + (svalue == null ? 0 : svalue.hashCode()) + (int) (dvalue * 10000));
        }

        public String toString() {
            if (Feature.isNumerical[featureNum]) {
                return Feature.featureNames[featureNum] + " < " + dvalue;
            } else {
                return Feature.featureNames[featureNum] + " = " + svalue;
            }
        }
        

    }

    DecisionTree(ArrayList<Example> examples) {
    	HashSet<Integer> attributes = new HashSet<Integer>();
    	for(int i=0; i < Feature.featureNames.length; i++) {
    		attributes.add(i);
    	}
    	constructDecisionTree(examples, attributes, null);
    	if(PRUNE) {
    		pruneDecisionTree(this, this, examples);
    	}
    }
    
    DecisionTree(ArrayList<Example> examples, HashSet<Integer> attributesAvailable, 
    		ArrayList<Example> parentExamples){
    	constructDecisionTree(examples, attributesAvailable, parentExamples);
    }


	public DecisionTree constructDecisionTree(ArrayList<Example> examples, HashSet<Integer> attributesAvailable, ArrayList<Example> parentExamples) {
		System.out.println("Entering construct decision tree method again");
//		System.out.println(examples);
		boolean exceptionalCase = true;
    	if(examples.size() == 0) {
    		System.out.println("No examples found");
    		this.feature = null;
    		this.decision = pluralityValue(parentExamples);
    		return this;
    	} else if(isSameClass(examples)) {
    		this.feature = null;
    		this.decision = examples.get(0).target;
    		System.out.println("All same class examples found    "+ this.decision);
    		return this;
    	} else if(attributesAvailable.size() == 1) {
    		System.out.println("No attributes found to classify");
    		this.feature = null;
    		this.decision = pluralityValue(examples);
    		return this;
    	} else {
    		int bestAvailableAttribute =-1;
    		double bestImportanceValue = Double.MAX_VALUE;
    		double bestAttributeValue = Double.MAX_VALUE;
    		String bestAttributeStringValue = "";
    		double currentAttributeImportance;
    		for(int attributeNumber : attributesAvailable) {
    			System.out.println("attributesAvailable: " + attributesAvailable);
    			if(Feature.featureNames[attributeNumber].equals("Target")) { 
    				continue;
    			}
    			if(Feature.isNumerical[attributeNumber]) {
//    				if feature is numerical, get all the possible value of current attribute and 
//    				get the best possible value of this current attribute
    				HashSet<Double> curAttributeSet = getDistinctValueOfAttribute(examples, attributeNumber);
    				for(double dValue: curAttributeSet) {
    					if(curAttributeSet.size() == 1 && attributesAvailable.size() ==2) {
    						bestAvailableAttribute = attributeNumber;
    						bestAttributeValue = dValue;
    						exceptionalCase=true;
    						System.out.println("Enabling exceptional case");
    					}else {
    						currentAttributeImportance = importance(examples, attributeNumber, dValue);
        					System.out.println(currentAttributeImportance + "  "+ bestImportanceValue + "   "+ dValue);
        					if(currentAttributeImportance < bestImportanceValue) {
        	    				bestImportanceValue = currentAttributeImportance;
        	    				bestAvailableAttribute = attributeNumber;
        	    				bestAttributeValue = dValue;
        	    			}
    					}
    				}
    			} else {
//    				if feature is not numerical, get all the possible value of current attribute and 
//    				get the best possible value of this current attribute
    				HashSet<String> curAttributeSet = getDistinctStringsOfAttribute(examples, attributeNumber);
    				for(String sValue : curAttributeSet ) {
    					if(curAttributeSet.size() == 1 && attributesAvailable.size() ==2) {
    						bestAvailableAttribute = attributeNumber;
    						bestAttributeStringValue = sValue;
    						exceptionalCase=true;
    					} else {
    						currentAttributeImportance = importance(examples, attributeNumber, sValue);
        					System.out.println(currentAttributeImportance + "  "+ bestImportanceValue + "  " + sValue);
        					if(currentAttributeImportance < bestImportanceValue) {
        	    				bestImportanceValue = currentAttributeImportance;
        	    				bestAvailableAttribute = attributeNumber;
        	    				bestAttributeStringValue = sValue;
        	    			}
    					}
    				}
    			}
    		}
//			by this time we have 
//			bestImportanceValue , bestAvailableAttribute, bestAttributeValue, bestAttributeStringValue
    		System.out.println("Outputing the best values");
    		System.out.println(bestImportanceValue + "  " + bestAvailableAttribute + "  "+ bestAttributeValue + "  " + 
    				bestAttributeStringValue);
    		if(Feature.isNumerical[bestAvailableAttribute]) {
    			feature = new Feature(bestAvailableAttribute, bestAttributeValue);
    			ArrayList<Example> vkExamples = new ArrayList<Example>();
    			ArrayList<Example> notVkExamples = new ArrayList<Example>();
    			for(Example example:examples) {
    				if(example.numericals[bestAvailableAttribute] < bestAttributeValue) {
    					vkExamples.add(example);
    				} else {
    					notVkExamples.add(example);
    				}
    			}
    			
    			if(exceptionalCase) {
    				System.out.println("Creating a no branch");
    				attributesAvailable.remove(bestAvailableAttribute);
    				noBranch = new DecisionTree(notVkExamples, attributesAvailable, examples);
        			System.out.println("Creating a yes branch and attributes available is " + attributesAvailable.size());
        			yesBranch =  new DecisionTree(vkExamples, attributesAvailable, examples);
    			} else {
    				System.out.println("Creating a no branch");
    				noBranch = new DecisionTree(notVkExamples, attributesAvailable, examples);
        			attributesAvailable.remove(bestAvailableAttribute);
        			System.out.println("Creating a yes branch and attributes available is " + attributesAvailable.size());
        			yesBranch =  new DecisionTree(vkExamples, attributesAvailable, examples);
    			}
				
    			
    		} else {
//    			System.out.println("Inside String Block");
    			/*
    			vkStringSet = getDistinctStringsOfAttribute(examples, bestAvailableAttribute);
    			Iterator<String> vkStringIterator = vkStringSet.iterator();
    			String vk = vkStringIterator.next();
    			*/
    			feature = new Feature(bestAvailableAttribute, bestAttributeStringValue);
    			ArrayList<Example> vkExamples = new ArrayList<Example>();
    			ArrayList<Example> notVkExamples = new ArrayList<Example>();
    			for(Example example:examples) {
    				if(example.strings[bestAvailableAttribute].equals(bestAttributeStringValue)) {
    					vkExamples.add(example);
    				} else {
    					notVkExamples.add(example);
    				}
    			}
    			/*
    			if(vkStringSet.size() > 2) {
//    				System.out.println("vkStringIterator size is more than 2");
//    				System.out.println("Creating a no branch");
    				noBranch = new DecisionTree(notVkExamples, attributesAvailable, examples);
        			attributesAvailable.remove(bestAvailableAttribute);
//        			System.out.println("Creating a yes branch and attributes available is " + attributesAvailable.size());
        			yesBranch =  new DecisionTree(vkExamples, attributesAvailable, examples);
    			} else {
//    				System.out.println("vkStringIterator size is 2");
    				attributesAvailable.remove(bestAvailableAttribute);
//    				System.out.println("Creating a no branch");
    				noBranch = new DecisionTree(notVkExamples, attributesAvailable, examples);
//    				System.out.println("Creating a yes branch and attributes available is " + attributesAvailable.size());
        			yesBranch =  new DecisionTree(vkExamples, attributesAvailable, examples);
    			} 
    			*/
    			
//				System.out.println("Creating a no branch");
				noBranch = new DecisionTree(notVkExamples, attributesAvailable, examples);
    			attributesAvailable.remove(bestAvailableAttribute);
//    			System.out.println("Creating a yes branch and attributes available is " + attributesAvailable.size());
    			yesBranch =  new DecisionTree(vkExamples, attributesAvailable, examples);
    		}			
    		
    	}
    	return this;
    }
	/*
	double importance(ArrayList<Example> examples, int attributeNmbr) {
		int total = examples.size();
		double returnVal=0;
		double curProb = 0.0;
		HashMap<Double, Integer> vkMap;
		HashMap<String, Integer> vkStringMap;
		if(Feature.isNumerical[attributeNmbr]) {
//			System.out.println("Checked whether attribute is numerical or not");
			vkMap = getMapDistinctValueOfAttribute(examples, attributeNmbr);
			for(Double key:vkMap.keySet()) {
				curProb = (double) vkMap.get(key)/total;
				if(curProb != 0)
					returnVal += (curProb * ((double) Math.log(2)/Math.log(curProb)));
//				System.out.println("curProb: " + curProb + "   " +  "returnVal: " + returnVal);
			}
		} else {
//			System.out.println("Checked whether attribute is numerical or not");
			vkStringMap = getMapDistinctStringsOfAttribute(examples, attributeNmbr);
			for(String key:vkStringMap.keySet()) {
				curProb = (double) vkStringMap.get(key)/total;
				if(curProb != 0)
					returnVal += (curProb * ((double) Math.log(2)/Math.log(curProb)));
//				System.out.println("curProb: " + curProb + "   " +  "returnVal: " + returnVal);
			}
		}
//		System.out.println("returnVal: " + returnVal);
		return (double) returnVal;
	}
	*/
	double importance(ArrayList<Example> examples, int attributeNmbr, double dValue) {
		System.out.println("Working for attribute: " + Feature.featureNames[attributeNmbr] + " and value " + dValue);
		int total = examples.size();
		ArrayList<Example> vkExamples = getVkExamples(examples, attributeNmbr, dValue);
//		System.out.println(" vkExamples in numerical importance are: " + vkExamples);
		ArrayList<Example> notVkExamples = getNotVkExamples(examples, attributeNmbr, dValue);
//		System.out.println(" notVkExamples in importance are: " + notVkExamples);
		return getEntropyVal(vkExamples, notVkExamples, total);
	}
	
	double importance(ArrayList<Example> examples, int attributeNmbr, String sValue) {
		System.out.println("Working for attribute: " + Feature.featureNames[attributeNmbr] + " and value " + sValue);
		int total = examples.size();
		ArrayList<Example> vkExamples = getVkStringExamples(examples, attributeNmbr, sValue);
//		System.out.println(" vkExamples in string importance are: " + vkExamples);
		ArrayList<Example> notVkExamples = getNotVkStringExamples(examples, attributeNmbr, sValue);
//		System.out.println(" vkExamples in importance are: " + vkExamples);
		return getEntropyVal(vkExamples, notVkExamples, total);
	}
	
	double getEntropyVal(ArrayList<Example> vkExamples, ArrayList<Example> notVkExamples, int total) {
		double returnVal=0;
		int vkYesCount = 0;
		int NotVkYesCount = 0;
		double probVk = (double) vkExamples.size()/total;
		System.out.println("probVk: " + probVk);
		double probNotVk = (double) notVkExamples.size()/total;
		System.out.println("probNotVk: " + probNotVk);
		for(Example example : vkExamples) {
			if(example.target)
				vkYesCount++;
		}
		for(Example example : notVkExamples) {
			if(example.target)
				NotVkYesCount++;
		}
		System.out.println("vkYesCount: " + vkYesCount);
		System.out.println("NotVkYesCount: " + NotVkYesCount);
		double probVkYes = (double) vkYesCount/vkExamples.size();
		System.out.println("probVkYes: " + probVkYes);
		double probVkNo = 1-probVkYes;
		System.out.println("probVkNo: " + probVkNo);
		double probNotVkYes = (double) NotVkYesCount/notVkExamples.size();
		System.out.println("probNotVkYes: " + probNotVkYes);
		double probNotVkNo = 1-probNotVkYes;
		System.out.println("probNotVkNo: " + probNotVkNo);
		double logProbVkYes = probVkYes==0 ? 0 : (probVkYes * (-1 * (Math.log(probVkYes)/Math.log(2))));
		System.out.println("logProbVkYes: " + logProbVkYes);
		double logProbVkNo = probVkNo == 0 ? 0 : (probVkNo * (-1 * (Math.log(probVkNo)/Math.log(2))));
		System.out.println("logProbVkNo: " + logProbVkNo);
		double logProbNotVkYes = probNotVkYes == 0 ? 0 : (probNotVkYes * (-1 * (Math.log(probNotVkYes)/Math.log(2))));
		System.out.println("logProbNotVkYes: " + logProbNotVkYes);
		double logProbNotVkNo = probNotVkNo == 0 ? 0 : (probNotVkNo * (-1 * (Math.log(probNotVkNo)/Math.log(2))));
		System.out.println("logProbNotVkNo: " + logProbNotVkNo);
		returnVal = probVk * ( logProbVkYes  + logProbVkNo );
		returnVal += probNotVk * ( logProbNotVkYes + logProbNotVkNo);
		returnVal = returnVal == 0 ? Double.MAX_VALUE : returnVal;
		System.out.println("returnVal: " + returnVal);
		return returnVal;
	}
	
	ArrayList<Example> getVkExamples(ArrayList<Example> examples, int attributeNmbr, double dValue){
		ArrayList<Example> returnSet = new ArrayList<Example>();
		for(Example example:examples) {
			if(example.numericals[attributeNmbr] < dValue)
				returnSet.add(example);
		}
		return returnSet;
	}
	
	ArrayList<Example> getNotVkExamples(ArrayList<Example> examples, int attributeNmbr, double dValue){
		ArrayList<Example> returnSet = new ArrayList<Example>();
		for(Example example:examples) {
			if(example.numericals[attributeNmbr] >= dValue)
				returnSet.add(example);
		}
		return returnSet;
	}
	
	ArrayList<Example> getVkStringExamples(ArrayList<Example> examples, int attributeNmbr, String sValue){
		ArrayList<Example> returnSet = new ArrayList<Example>();
		for(Example example:examples) {
			if(example.strings[attributeNmbr].equals(sValue))
				returnSet.add(example);
		}
		return returnSet;
	}
	
	ArrayList<Example> getNotVkStringExamples(ArrayList<Example> examples, int attributeNmbr, String sValue){
		ArrayList<Example> returnSet = new ArrayList<Example>();
		for(Example example:examples) {
			if(!example.strings[attributeNmbr].equals(sValue))
				returnSet.add(example);
		}
		return returnSet;
	}
	
	HashSet<Double> getDistinctValueOfAttribute(ArrayList<Example> examples, int attributeNmbr) {
		HashSet<Double> set = new HashSet<Double>();
		for(Example example:examples)
			set.add(example.numericals[attributeNmbr]);
    	return set;
    }
    
	HashSet<String> getDistinctStringsOfAttribute(ArrayList<Example> examples, int attributeNmbr) {
		HashSet<String> set = new HashSet<String>();
		for(Example example:examples)
			set.add(example.strings[attributeNmbr]);
    	return set;
    }
	
	HashMap<Double, Integer> getMapDistinctValueOfAttribute(ArrayList<Example> examples, int attributeNmbr) {
		HashMap<Double, Integer> map = new HashMap<Double, Integer>();
		for(Example example:examples) {
			map.put(example.numericals[attributeNmbr], 
					map.getOrDefault(example.numericals[attributeNmbr], 1) + 1);
		}
    	return map;
    }
    
	HashMap<String, Integer> getMapDistinctStringsOfAttribute(ArrayList<Example> examples, int attributeNmbr) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for(Example example:examples)
			map.put(example.strings[attributeNmbr], 
					map.getOrDefault(example.strings[attributeNmbr], 1) + 1);
    	return map;
    }
	
	Boolean isSameClass(ArrayList<Example> examples) {
		Boolean firstVal = examples.get(0).target;
		for(Example example:examples)
			if(example.target != firstVal)
				return false;
		return true;
	}
	
	Boolean pluralityValue(ArrayList<Example> examples) {
		int trueCount = 0;
		int falseCount = 0;
		for(Example example:examples) 
			if(example.target)
				trueCount++;
			else
				falseCount++;
		return trueCount >= falseCount;
	}

    public static class Results {
        public int true_positive;
        public int true_negative;
        public int false_positive;
        public int false_negative;

        public Results() {
            true_positive = 0;
            true_negative = 0;
            false_positive = 0;
            false_negative = 0;
        }

        public String toString() {
            String out = "Precision: ";
            out += String.format("%.4f", true_positive/(double)(true_positive + false_positive));
            out += "\nRecall: " + String.format("%.4f",true_positive/(double)(true_positive + false_negative));
            out += "\n";
            out += "Accuracy: ";
            out += String.format("%.4f", (true_positive + true_negative)/(double)(true_positive + true_negative + false_positive + false_negative));
            out += "\n";
            return out;
        }
    }
    
    public Results classify(ArrayList<Example> examples) {
        Results results = new Results();
        Boolean actualVal;
        Boolean predictedVal;
        for(Example example:examples) {
        	actualVal = example.target;
        	predictedVal = getDecision(this, example);
        	
        	if(predictedVal && actualVal)
        		results.true_positive++;
        	else if(predictedVal && !actualVal)
        		results.false_positive++;
        	else if(!predictedVal && !actualVal)
        		results.true_negative++;
        	else if(!predictedVal && actualVal)
        		results.false_negative++;
        }
        return results;
    }
    
    public Boolean getDecision(DecisionTree dt, Example example) {
    	if(dt.feature == null)
    		return dt.decision;
    	if(dt.feature.apply(example)) {
    		return getDecision(dt.yesBranch, example);
    	}
    	else {
    		return getDecision(dt.noBranch, example);
    	}
    }

    
    DecisionTree pruneDecisionTree(DecisionTree Origdt, DecisionTree dt, ArrayList<Example> examples) {
    	if(dt.yesBranch == null && dt.noBranch == null)
    		return dt;
    	ArrayList<Example> yesExamples = new ArrayList<Example>();
    	ArrayList<Example> noExamples = new ArrayList<Example>();
    	for(Example example: examples) {
    		if(dt.feature.apply(example))
    			yesExamples.add(example);
    		else
    			noExamples.add(example);
    	}
    	dt.yesBranch = pruneDecisionTree(Origdt, dt.yesBranch, yesExamples); 
    	dt.noBranch = pruneDecisionTree(Origdt, dt.noBranch, noExamples);
    	if(dt.yesBranch.feature == null && dt.noBranch.feature == null)
    		dt = applyChiSquareTest(Origdt, dt, examples);
    	return dt;
    }
    
    DecisionTree applyChiSquareTest(DecisionTree Origdt, DecisionTree dt, ArrayList<Example> examples) {
    	int expectedYesCount = 0;
    	int expectedNoCount = 0;
    	int observedYesCount = 0;
    	int observedNoCount = 0;
    	int total = examples.size();
    	for(Example example : examples) {
    		if(example.target)
    			expectedYesCount++;
    		else
    			expectedNoCount++;
    		if(getDecision(Origdt, example))
    			observedYesCount++;
    		else
    			observedNoCount++;
    	}
//    	double expectedProb = 0.5;
    	double expectedYesProb = expectedYesCount/total;
    	double expectedNoProb = expectedNoCount/total;
    	double observedYesProb = observedYesCount/total;
    	double observedNoProb = observedNoCount/total;
    	double result = expectedYesProb * observedYesProb;
    	result +=  expectedYesProb * observedNoProb;
    	result += expectedNoProb * observedYesProb;
    	result += expectedNoProb * observedNoProb;
    	result = 4 * result;
    	if(result >= CHI_THRESH)
    		return dt;
    	else {
    		dt.decision = expectedYesCount >= expectedNoCount;
    		dt.feature = null;
    		dt.yesBranch = null;
    		dt.noBranch = null; 
    		return dt;
    	}		
    }
    
    public String toString() {
        return toString(0);
    }

    public String toString(int depth) {
        String out = "";
        for (int i = 0; i < depth; i++) {
            out += "    ";
        }
        if (feature == null) {
            out += (decision ? "YES" : "NO");
            out += "\n";
            return out;
        }
        out += "if " + feature + "\n";
        out += yesBranch.toString(depth+1);
        for (int i = 0; i < depth; i++) {
            out += "    ";
        }
        out += "else\n";
        out += noBranch.toString(depth+1);
        return out;
    }

}
