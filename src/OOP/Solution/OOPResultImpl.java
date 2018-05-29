package OOP.Solution;

import OOP.Provided.OOPResult;

public class OOPResultImpl implements OOPResult {

    //Attribute: a test method's result
    private OOPTestResult result;

    //Attribute: a test method's name
    private String message;

    OOPResultImpl(OOPTestResult result, String message) {
        this.result = result;
        this.message = message;
    }

    @Override
    public OOPTestResult getResultType() {
        return result;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof OOPResultImpl)) {
            return false;
        }
        OOPResultImpl compareTo = (OOPResultImpl) obj;
        return compareTo.message.equals(message) && compareTo.result.equals(result);
    }

    @Override
    public int hashCode() {
       return message.hashCode() * 4 + result.ordinal();
    }
}
