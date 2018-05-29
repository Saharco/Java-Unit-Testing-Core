package OOP.Solution;

import OOP.Provided.OOPResult;

import java.util.HashMap;
import java.util.Map;

/**
 * This class maps each test method's name to its corresponding result.
 * Provides functionally to count the amount of each desired result type.
 * @see OOPResult
 */
public class OOPTestSummary {

    //Attribute: a dictionary which maps the test method's result to each method's name
    private Map<String, OOPResult> testMap;

    /**
     * Helper function which is used in order to count the amount of a given result type
     * @param result: the desired result to be counted
     * @return the amount of OOPTest methods mapped to the given result
     */
    private int countResults(OOPResult.OOPTestResult result) {
        int count = 0;
        for(OOPResult testResult : testMap.values()) {
            if(testResult.getResultType() == result) {
                //The test method terminated with the desired result: increase counter
                count++;
            }
        }
        return count;
    }

    OOPTestSummary (Map<String, OOPResult> testMap) {
        this.testMap = testMap;
    }

    /**
     * @return the amount of tests that terminated with SUCCESS
     */
    public int getNumSuccesses() {
        return countResults(OOPResult.OOPTestResult.SUCCESS);
    }

    /**
     * @return the amount of tests that terminated with FAILURE
     */
    public int getNumFailures() {
        return countResults(OOPResult.OOPTestResult.FAILURE);

    }

    /**
     * @return the amount of tests that terminated with EXPECTED_EXCEPTION_MISMATCH
     */
    public int getNumExceptionMismatches() {
        return countResults(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH);
    }

    /**
     * @return the amount of tests that terminated with ERROR
     */
    public int getNumErrors() {
        return countResults(OOPResult.OOPTestResult.ERROR);
    }
}
