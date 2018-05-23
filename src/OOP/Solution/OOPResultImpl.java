package OOP.Solution;

import OOP.Provided.OOPResult;

public class OOPResultImpl implements OOPResult {

    @Override
    public OOPTestResult getResultType() {
        OOPTestResult result = null; //TODO: init this
        return result;
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false; //TODO: FIX
    }

    @Override
    public int hashCode() {
        return 0; //TODO: FIX
    }
}
