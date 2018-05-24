package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;

import java.lang.reflect.*;

public class OOPUnitCore {

    public static void assertEquals(Object expected, Object actual) throws OOPAssertionFailure {
        if(!expected.equals(actual)) {
            throw new OOPAssertionFailure();
        }
    }

    public static void fail() throws OOPAssertionFailure {
        throw new OOPAssertionFailure();
    }

    public static OOPTestSummary runClass(Class<?> testClass) throws IllegalArgumentException {
        return null; //TODO: implement me
    }

    public static OOPTestSummary runClass(Class<?> testClass, String tag)
            throws IllegalArgumentException {
        Class c = testClass.getClass();
        c copy;
        try {
            Constructor ctor = testClass.getConstructor();

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
