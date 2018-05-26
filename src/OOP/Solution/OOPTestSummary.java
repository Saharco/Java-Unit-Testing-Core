package OOP.Solution;

import OOP.Provided.OOPResult;

import java.util.HashMap;
import java.util.Map;

public class OOPTestSummary {

    private Map<String, OOPResult> testMap;

    private int countResults(OOPResult.OOPTestResult result) {
        int count = 0;
        for(OOPResult testResult : testMap.values()) {
            if(testResult.getResultType() == result) {
                count++;
            }
        }
        return count;
    }

    OOPTestSummary (Map<String, OOPResult> testMap) {
        this.testMap = testMap;
    }

    public int getNumSuccesses() {
        return countResults(OOPResult.OOPTestResult.SUCCESS);
    }

    public int getNumFailures() {
        return countResults(OOPResult.OOPTestResult.FAILURE);

    }

    public int getNumExceptionMismatches() {
        return countResults(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH);
    }

    public int getNumErrors() {
        return countResults(OOPResult.OOPTestResult.ERROR);
    }
}
