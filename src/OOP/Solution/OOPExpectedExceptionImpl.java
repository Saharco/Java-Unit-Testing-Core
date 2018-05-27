package OOP.Solution;

import OOP.Provided.OOPExpectedException;

import java.util.LinkedList;
import java.util.List;

public class OOPExpectedExceptionImpl implements OOPExpectedException {

    //Attribute: the expected exception class
    private Class<? extends Exception> expected;
    //Attribute: message string that we expect in the exception's message
    private String message;

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
        message = msg;
        return this;
    }

    @Override
    public boolean assertExpected(Exception e) {
        Class<?> clazz = e.getClass();
        if(!(e.getClass().isAssignableFrom(expected))) {
            //e is not a subclass of the expected exception
            return false;
        }
        String exceptionMessage = e.getMessage();
        if((message == null && exceptionMessage != null) || !exceptionMessage.contains(message)) {
            //the message does not contain the given sub-message
            return false;
        }
        return true;
    }

    public static OOPExpectedExceptionImpl none () {
        //Create a new expected exception, where the expected exception is null
        OOPExpectedExceptionImpl res = new OOPExpectedExceptionImpl();
        res.expected = null;
        res.message = null;
        return res;
    }
}