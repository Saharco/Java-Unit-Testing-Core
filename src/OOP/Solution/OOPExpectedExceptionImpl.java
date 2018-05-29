package OOP.Solution;

import OOP.Provided.OOPExpectedException;

import java.util.LinkedList;
import java.util.List;

public class OOPExpectedExceptionImpl implements OOPExpectedException {

    //Attribute: the expected exception class
    private Class<? extends Exception> expected;
    //Attribute: message string that we expect in the exception's message
    private List<String> messages;

    @Override
    public Class<? extends Exception> getExpectedException() {
        return expected;
    }

    @Override
    public OOPExpectedException expect(Class<? extends Exception> expected) {
        this.expected = expected;
        return this;
    }

    @Override
    public OOPExpectedException expectMessage(String msg) {
        messages.add(msg);
        return this;
    }

    @Override
    public boolean assertExpected(Exception e) {
        if(!(expected.isAssignableFrom(e.getClass()))) {
            return false;
        }
        String exceptionMessage = e.getMessage();
        if(exceptionMessage == null) {
            return messages.isEmpty();
        }
        for(String message : messages) {
            if(!exceptionMessage.contains(message)) {
                return false;
            }
        }
        return true;
    }

    public static OOPExpectedExceptionImpl none () {
        //Create a new expected exception, where the expected exception is null
        OOPExpectedExceptionImpl res = new OOPExpectedExceptionImpl();
        res.expected = null;
        res.messages = new LinkedList<>();
        return res;
    }
}