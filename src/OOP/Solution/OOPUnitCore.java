package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The main OOPUnit framework's class. Utilizes reflection to run tests in the desired order, and
 * gathers the results for all the class tests.
 * This class also utilizes the following OOPUnit classes & annotations:
 * @see OOPTestClass: annotates a test class
 * @see OOPSetup: annotates a setup method
 * @see OOPBefore: annotates a method to be called before tests
 * @see OOPAfter: annotates a method to be called after tests
 * @see OOPTest: annotates a test method
 * @see OOPExceptionRule: annotates fields from the type OOPExpectedException
 * @see OOPExpectedExceptionImpl: wraps an exception type that's expected from this test, along
 *      with its expected message (as invoked by {@code e.getMessage()})
 * @see OOPResultImpl: wraps a test method's result type and an appropriate message for the test.
 *      @see OOP.Provided.OOPResult.OOPTestResult for possible result types
 * @see OOPTestSummary: dictionary that hold the results of all the tests.
 *      maps each of the test class's test methods with its corresponding OOPResult
 *
 * ************************************************************************************************
 *
 * The OOPUnit API:
 *
 * {@link #assertEquals(Object, Object)}
 *  throws an OOPAssertionFailure exception iff two given objects aren't identical
 * {@link #fail()}
 *  throws an OOPAssertionFailure
 * {@link #runClass(Class)}
 *  runs all the OOPUnit annotated setup methods, before methods, test methods and after methods
 * {@link #runClass(Class, String)}
 *  runs the tagged OOPUnit annotated test methods that match with the given tag
 *
 *  *************************** Helper functions to support this class: ***************************
 *
 *  Information gathering functions:
 *
 *  {@link #getOOPUnitAnnotation(Method)}
 *  {@link #getOOPExceptionRules(Class)}
 *  {@link #classOOPMethods(Class)}
 *  {@link #sortOOPTests(List, Class, String)}
 *  {@link #initCopy(Class)}  //TODO: check if this is OK
 *  {@link #expectedException(List, Exception, Object)}
 *  {@link #containsTest(Method, Class, Method)}
 *
 *  ***********************************************************************************************
 *
 *  Methods invoking functions:
 *
 *  Invokes the OOPSetup methods: {@link #callSetupMethods(Map, Object)}
 *  Invokes all OOPTest methods: {@link #callTestMethods(Map, Field, Object, Map)}
 *  Invokes a given OOPTest method's corresponding OOPBefore or OOPAfter methods:
 *      {@link #callBeforeAfter(Map, Object, Class, Method)}
 *
 *  ***********************************************************************************************
 *
 *  Backup related functions:
 *
 *  {@link #backup(Object)}
 *  {@link #fieldBackup(Object)}
 *  {@link #copyObjectFields(Object, Object)}
 *
 *  ***********************************************************************************************
 *
 *  Misc functions:
 *
 *  {@link #reverseArray(Object[])}
 *
 *  ***********************************************************************************************
 *
 */
public class OOPUnitCore {

    //Attribute: the default tag for tests that are not tagged
    private final static String defaultTag = "";

    public static void assertEquals(Object expected, Object actual) throws OOPAssertionFailure {
        if((expected == null && actual != null) ||
                ((expected != null) && !(expected.equals(actual)))) {
            throw new OOPAssertionFailure();
        }
    }

    public static void fail() throws OOPAssertionFailure {
        throw new OOPAssertionFailure();
    }

    public static OOPTestSummary runClass(Class<?> testClass) throws IllegalArgumentException {
        return runClass(testClass, defaultTag);
    }

    public static OOPTestSummary runClass(Class<?> testClass, String tag)
            throws IllegalArgumentException {

        if(testClass == null || testClass.getAnnotationsByType(OOPTestClass.class).length == 0) {
            //Given class is either not a class, or not an OOPUnit test class
            throw new IllegalArgumentException();
        }

        //The result map of all the tests
        Map<String,OOPResult> OOPTestsResults = new HashMap<>();

        //The methods map, which maps a list of OOP annotated methods to each annotation type
        Map<Class <? extends Annotation>, List<Method>> annotatedMethods =
                getOOPMethods(testClass, tag);

        //The class's expected exception field annotated by OOPExceptionRule (null if doesn't exist)
        Field expectedException = getOOPExceptionField(testClass);

        //A copy of the given class object: initialized with the given class's 0-args constructor
        Object copyObject = initCopy(testClass);

        //Run all of the OOPSetup annotated methods, excluding overridden methods
        callSetupMethods(annotatedMethods, copyObject);

        /*
         * Run the appropriate test methods in the desired order, and gather the results.
         * Calls test-appropriate OOPBefore & OOPAfter methods for each of the tests
         */

        try {
            callTestMethods(annotatedMethods, expectedException, copyObject,
                    OOPTestsResults);
        } catch(Exception e) {
            //We shouldn't get here
            fail();
        }

        return new OOPTestSummary(OOPTestsResults);
    }

    private static Field getOOPExceptionField(Class<?> testClass) {
        Class<?> current = testClass;
        while(current != null) {
            for(Field field : current.getDeclaredFields()) {
                if(field.getAnnotation(OOPExceptionRule.class) != null) {
                    field.setAccessible(true);
                    if (field.getClass().isAssignableFrom(OOPExpectedException.class)) {
                        //Assumption: OOPExceptionRule only annotates OOPExpectedException fields
                        fail();
                    }
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }


    //TODO: Delete this (?)
    /**
     * Returns a list of this test class's exception rules
     * @param testClass: the test class
     * @return a list of every field annotated by OOPExceptionRule. Each of these fields is of type
     * OOPExpectedException
     */
    private static List<Field> getOOPExceptionRules(Class<?> testClass) {
        List<Field> fields = new LinkedList<>();
        Class<?> current = testClass;
        while(current.getSuperclass() != null) {
            for(Field field : current.getDeclaredFields()) {
                if(field.getAnnotation(OOPExceptionRule.class) != null) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Main framework method: runs all the tests in the test class, and gathers the results
     * @param annotatedMethods: dictionary that consists of a list of OOPUnit annotated methods,
     *                        in the order in which they should run, for each of the annotation types.
     * @param expectedException: the class's expected exception field
     * @param copyObject: class on which the tests will be invoked
     * @param OOPTestsResults: method_name -> OOPResult dictionary that marks the results of all
     *                       the test methods.
     */
    private static void callTestMethods(Map<Class<? extends Annotation>, List<Method>>
                                      annotatedMethods, Field expectedException,
                                      Object copyObject, Map<String,OOPResult> OOPTestsResults) {
        Object backupObject = backup(copyObject);
        for(Method test : annotatedMethods.get(OOPTest.class)) {
            //Run OOPBefore methods:
            try {
                callBeforeAfter(annotatedMethods, copyObject, OOPBefore.class, test);
            } catch (Exception e) {
               /*
                * The test has failed: couldn't run OOPBefore methods.
                * Mark the test's failure, restore the object, and continue to the next test
                */
                OOPTestsResults.put(test.getName(), new
                        OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getMessage()));
                copyObjectFields(copyObject, backupObject);
                continue;
            } catch (Throwable throwable) {
                //We shouldn't get here
                fail();
            }
            //Run Tests:
            //We reset the expected exception before each test
            resetExpectedException(expectedException, copyObject);
            OOPExpectedException rule = OOPExpectedException.none();
            try {
                test.invoke(copyObject); //Might also change the expected exception
                //The test succeeded. We mark this as a success,
                // but will override in case of failure in OOPAfter methods
                OOPTestsResults.put(test.getName(), new OOPResultImpl(
                        OOPResult.OOPTestResult.SUCCESS, null));
            } catch(InvocationTargetException e) {
                try {
                    //Method threw an exception: decipher which exception it was!
                    //We throw the exception that's wrapped inside e forward, and catch it outside
                    throw e.getCause();

                } catch(OOPAssertionFailure exception) {
                    OOPResult testResult = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE,
                            e.getMessage());
                    OOPTestsResults.put(test.getName(), testResult);
                    copyObjectFields(copyObject, backupObject);
                } catch(Exception exception) {
                    rule = getExpectedException(expectedException, copyObject);
                    if (rule.getExpectedException() == null) {
                        //Unexpected exception occurred: Error!

                        OOPTestsResults.put(test.getName(), new OOPResultImpl(
                                OOPResult.OOPTestResult.ERROR, exception.getClass().getName()));
                        copyObjectFields(copyObject, backupObject);
                    } else if (rule.assertExpected(exception)) {
                        //Expected exception: Success!

                        OOPTestsResults.put(test.getName(), new OOPResultImpl(
                                OOPResult.OOPTestResult.SUCCESS, null));
                    } else {
                        //Expected exception mismatch!

                        try {
                            OOPTestsResults.put(test.getName(), new OOPResultImpl(
                                    OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH, new
                                    OOPExceptionMismatchError(rule.getExpectedException(),
                                    exception.getClass()).getMessage()));
                        } catch (Exception exe) {
                            //We shouldn't get here
                            fail();
                        }
                        copyObjectFields(copyObject, backupObject);
                    }
                } catch (Throwable throwable) {
                    //We shouldn't get here
                    fail();
                }
            } catch(Exception e) {
                //We shouldn't get here
                fail();
            }
            //Run OOPAfter methods:
            try {
                callBeforeAfter(annotatedMethods, copyObject, OOPAfter.class, test);
            } catch (Exception e) {
                /*
                 * The test has failed: couldn't run OOPAfter methods.
                 * Mark the test's failure, restore the object, and continue to the next test
                 */
                OOPTestsResults.put(test.getName(), new OOPResultImpl(OOPResult.OOPTestResult.ERROR,
                        e.getClass().getName())); //This will override the result
                copyObjectFields(copyObject, backupObject);
            } catch (Throwable throwable) {
                //We shouldn't get here
                fail();
            }
        }
    }

    private static OOPExpectedException getExpectedException(Field expectedException,
                                                             Object copyObject) {
        OOPExpectedException expectEmpty = OOPExpectedException.none();
        if(expectedException == null) {
            return expectEmpty;
        }
        try {
            return (OOPExpectedException) expectedException.get(copyObject);
        } catch (IllegalAccessException e) {
            //We shouldn't get here
            fail();
        }
        return expectEmpty;
    }

    private static void resetExpectedException(Field expectedException, Object copyObject) {
        if(expectedException == null) {
            return;
        }
        try {
            expectedException.set(copyObject, OOPExpectedException.none());
        } catch (IllegalAccessException e) {
            //We shouldn't get here
            fail();
        }
    }


    /**
     * Checks if a given exception was declared in a given test class's exception rules
     * @param classExceptionRules: the class's exception rules
     * @param exception: a candidate exception to be checked
     * @param copyObject: the test class
     * @return true iff the given exception is part of the class's exception rules
     */
    private static boolean expectedException(List<Field> classExceptionRules, Exception exception,
                                             Object copyObject) {
        try {
            for(Field rule : classExceptionRules) {
                if(((OOPExpectedException)rule.get(copyObject)).assertExpected(exception)) {
                    //We expect this exception's type and message
                    return true;
                }
            }
        } catch(Exception e) {
            //We shouldn't get here
            fail();
        }
        return false;
    }

    /**
     * Runs either OOPBefore or OOPAfter annotated methods, and throws potential exceptions
     * from running these methods onwards in case they fail
     * @param annotatedMethods: dictionary of the OOPUnit methods, listed in the desired order
     * @param copyObject: the test class on which the methods will be invoked
     * @param annotation: invoked methods' annotation type: either OOPBefore or OOPAfter
     * @param test: the test methods that is currently being run with this set of OOPBefore and
     *            OOPAfter methods
     * @throws Exception: exception that might be thrown from any of the invoked methods
     */
    private static void callBeforeAfter(Map<Class<? extends Annotation>, List<Method>>
                                        annotatedMethods, Object copyObject,
                                        Class<? extends Annotation> annotation,
                                        Method test) throws Throwable {
        assert(annotation == OOPBefore.class || annotation == OOPAfter.class);
        Method[] methods = new Method[annotatedMethods.get(annotation).size()];
        methods = annotatedMethods.get(annotation).toArray(methods);
        if(annotation == OOPAfter.class) { //TODO: Check if this is OK
            reverseArray(methods);
        }
        Class<?> testClass = test.getDeclaringClass();
        List<Method> suitableMethods = Arrays.stream(methods)
                .filter(m -> (containsTest(m, annotation, test)) &&
                        (m.getDeclaringClass().isAssignableFrom(testClass))) //TODO: Check if this is OK
                .collect(Collectors.toList());
        for(Method m : suitableMethods) {
//            backupObject = backup(copyObject); TODO: check if backup is performed here
            try {
                m.invoke(copyObject);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } catch (IllegalAccessException e) {
                //We shouldn't get here
                fail();
            }
        }
    }

    /**
     * Checks if a given OOPBefore/OOPAfter method should be invoked for a given test method
     * @param m: the OOPBefore/OOPAfter method that needs to be checked
     * @param annotation: annotation type: either OOPBefore or OOPAfter
     * @param test: the test method
     * @return true iff the method's OOPBefore / OOPAfter annotation contains the given test
     * method's name
     */
    private static boolean containsTest(Method m, Class<? extends Annotation> annotation,
                                        Method test) {
        assert(annotation == OOPBefore.class || annotation == OOPAfter.class);
        return (annotation == OOPBefore.class) ?
                Arrays.asList(m.getDeclaredAnnotation(OOPBefore.class).value())
                        .contains(test.getName()) :
                Arrays.asList(m.getDeclaredAnnotation(OOPAfter.class).value())
                        .contains(test.getName());
    }

    /**
     * Generic function that reverses the order in which an array's elements are indexed
     * @param arr: array to be reversed
     */
    private static<T> void reverseArray(T[] arr) {
        List<T> orderList = Arrays.asList(arr);
        Collections.reverse(orderList);
        orderList.toArray(arr);
    }

    /**
     * Backs-up a given class's declared fields
     * @param copyObject: the class to be backed-up
     * @return a backup of the object, which is created by calling the given object's constructor
     * and setting its declared class fields in the following priority:
     * 1) if the field's class supports cloning: clone the field from the original object
     * 2) if the field's class has a copy constructor: invoke it
     * 3) store the original field of the given object
     */
    private static Object backup(Object copyObject) {
        Object backupObject = null;
        try {
            backupObject = initCopy(copyObject.getClass());
            copyObjectFields(backupObject, copyObject);
        } catch (Exception e) {
            //We shouldn't get here
            fail();
        }
        return backupObject;
    }

    /**
     * see: {@link #backup(Object)}
     * Copies all of the class's declared fields' values
     * @param target: target class to which the fields' values will be copied
     * @param source: source class from which the fields' values will be copied
     */
    private static void copyObjectFields(Object target, Object source) {
        try {
            for (Field field : source.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(target, fieldBackup(field.get(source)));
            }
        } catch (Exception e) {
            //We shouldn't get here
            fail();
        }
    }

    /**
     * Copies a class's field's value in the appropriate priority, as described in the backup method
     * @see #backup(Object)
     * @param field: the source field to be copied
     * @return a field that was copied from the given field
     */
    private static Object fieldBackup(Object field) {
        if(field == null) {
            return null;
        }
        Class<?> c = field.getClass();

        //Check if the object supports cloning:
        Class<?> current = field.getClass();
        while(current != null) {
            try {
                Method cloneMethod = current.getDeclaredMethod("clone");
                //Found a clone method
                cloneMethod.setAccessible(true);
                return cloneMethod.invoke(field); //Will fail if the field is not cloneable
            } catch (NoSuchMethodException e) {
                //Current class does not support cloning: keep traversing the hierarchy tree
            } catch (InvocationTargetException e) {
                //Can't clone the field: clone method threw CloneNotSupportedException
                break;
            } catch (IllegalAccessException e) {
                //We shouldn't get here
                fail();
            }
            current = current.getSuperclass();
        }

        //Check if the object has a copy constructor:
        try {
            Constructor<?> copyCtor = c.getDeclaredConstructor(c);
            copyCtor.setAccessible(true);
            return copyCtor.newInstance(field);
        } catch (Exception e) {
            //The object does not have a copy constructor
        }

        //Object supports neither cloning nor copy constructor: the backup is the field itself
        return field;
    }

    /**
     * Filters out and sorts the OOPTest methods in the order in which they should be invoked
     * @param OOPTests: list of the OOPTest methods gathered from the test class
     * @param testClass: the class on which the tests will be invoked
     * @param tag: the tag that all the invoked OOPTest methods should have
     * @return a sorted list of OOPTest methods, filtered by the given tag:
     * if the given tag is an empty string "" - all OOPTest methods are qualified.
     * test methods' order will take place only if the test class's OOPTestClass annotation is
     * marked with the ORDERED enum instance
     * @throws IllegalArgumentException: in case the class isn't OOPTestClass annotated
     */
    private static List<Method> sortOOPTests(List<Method> OOPTests, Class<?> testClass, String tag)
            throws IllegalArgumentException {
        if(testClass == null || testClass.getDeclaredAnnotation(OOPTestClass.class) == null) {
            //Given testClass is not annotated by OOPTestClass!
            throw new IllegalArgumentException();
        }

        assert(testClass.getDeclaredAnnotation(OOPTestClass.class) != null);
        if(testClass.getDeclaredAnnotation(OOPTestClass.class).value() ==
                OOPTestClass.OOPTestClassType.UNORDERED) {
            //No order is required for the test methods
            return OOPTests;
        }

        Method[] currentTestMethods = new Method[OOPTests.size()];
        return Arrays.stream(OOPTests.toArray(currentTestMethods))
                .filter(m->m.getDeclaredAnnotation(OOPTest.class).tag().equals(tag) ||
                        (!tag.equals(defaultTag)))
                .sorted(Comparator.comparingInt(m ->
                        m.getDeclaredAnnotation(OOPTest.class).order()))
                .collect(Collectors.toList());
    }

    /**
     * invokes all of class's the setup methods, with no defined order.
     * Assumption: the setup methods do not throw exceptions
     * @param annotatedMethods: dictionary of the OOPUnit methods, listed in the desired order
     * @param copyObject: the class on which the setup methods will be invoked
     */
    private static void callSetupMethods(Map<Class <? extends Annotation>, List<Method>>
                                                 annotatedMethods, Object copyObject) {
        try {
            /*
             * Run all of the OOPSetup annotated methods, starting with the top of the hierarchy
             * tree, excluding overridden methods
             */
            Method[] setupMethods = new Method[annotatedMethods.get(OOPSetup.class).size()];
            Arrays.stream(annotatedMethods.get(OOPSetup.class).toArray(setupMethods))
                    .forEach(m -> {
                        try {
                            m.invoke(copyObject);
                        } catch (Exception e) {
                            //We shouldn't get here
                            fail();
                        }
                    });
        } catch (Exception e) {
            //We shouldn't get here
            fail();
        }
    }

    /**
     * initializes the copy object of the given test class with the class's 0-args constructor
     * Assumption: the class has a 0-arguments constructor
     * @param testClass: the class to be copied
     * @return a new copy of the test class
     */
    private static Object initCopy(Class<?> testClass) {
        try {
            Constructor ctor = testClass.getDeclaredConstructor(); //We expect a 0-args constructor
            ctor.setAccessible(true); //Constructor might not be accessible
            return ctor.newInstance();
        } catch (Exception e) {
            //We shouldn't get here
            fail();
        }
        return null;
    }

    /**
     * Returns the correct OOPUnit annotation for a given method.
     * Assumption: a method will not be annotated by several OOPUnit annotations
     * @param m: the method to be checked
     * @return the correct OOPUnit annotation if exists, or null otherwise
     */
    private static Annotation getOOPUnitAnnotation(Method m) {
        //List of all the currently supported OOP annotations' names in the OOPUnit framework
        List<String> OOPUnitAnnotationsList = new LinkedList<>
                (Arrays.asList("OOPSetup", "OOPBefore", "OOPTest", "OOPAfter"));
        for (Annotation annotation : m.getDeclaredAnnotations()) {
            if(OOPUnitAnnotationsList.contains(annotation.annotationType().getSimpleName())) {
                //Found an OOPUnit annotation for this method
                return annotation;
            }
        }
        //The method is not part of the OOPUnit framework
        return null;
    }

    /**
     * Returns a list that consists of relevant methods' information of the OOPUnit methods in
     * the test class
     * @param c: the test class from which we collect the methods' information
     * @return a list of MethodInfo constructed as follows: the wrapper contains the method
     * itself for each of the found methods, along with its OOPUnit annotation type, its name,
     * and its access level
     */
    private static List<MethodInfo> classOOPMethods(Class<?> c) {
        Stack<MethodInfo> stack = new Stack<>();
        Class<?> current = c;
        while(current.getSuperclass() != null) {
            for(Method m : current.getDeclaredMethods()) {
                Annotation methodOOPAnnotation = getOOPUnitAnnotation(m);
                if(methodOOPAnnotation == null) {
                    //Current method isn't part of the OOPUnit framework
                    continue;
                }
                //Fill all of the MethodInfo wrapper class's information:
                MethodInfo currentMethodInfo = new MethodInfo(m,
                        Modifier.toString(m.getModifiers()), m.getName(), methodOOPAnnotation);
                if(stack.search(currentMethodInfo) == -1) {
                    //The current method should be invoked: add it to the stack
                    stack.push(currentMethodInfo);
                }
            }
            current = current.getSuperclass();
        }
        List<MethodInfo> allOOPMethods = new LinkedList<>();
        while(!stack.isEmpty()) {
            //Load method info from the stack into the list
            allOOPMethods.add(stack.pop());
        }
        return allOOPMethods;
    }

    /**
     * Returns a dictionary that consists of a list of methods for each OOPUnit annotation type.
     * @param c: the test class from which the OOPUnit methods are gathered from
     * @param tag: the tag that filters out OOPTests that shouldn't be invoked
     * @return a dictionary that consists of a list of methods for each OOPUnit annotation type.
     * the dictionary's methods are sorted according to the order in which they should be invoked.
     * additionally, the OOPTest methods are filtered out according to the given tag,
     * and sorted if the test class is set to be ordered
     */
    private static Map<Class<? extends Annotation>,List<Method>> getOOPMethods(Class<?> c,
                                                                                String tag) {
        Map<Class<? extends Annotation>,List<Method>> methodsDict = new HashMap<>();
        //Initialize an empty list of methods for each possible method annotation:
        methodsDict.put(OOPSetup.class, new LinkedList<>());
        methodsDict.put(OOPBefore.class, new LinkedList<>());
        methodsDict.put(OOPTest.class, new LinkedList<>());
        methodsDict.put(OOPAfter.class, new LinkedList<>());
        /*
         * Fill the lists with the appropriate methods for each annotation.
         * Lists are ordered from the top of the hierarchy tree, to the bottom
         */
        for(MethodInfo info : classOOPMethods(c)) {
            // Add all the OOPUnit annotated methods in the class's hierarchy to the dictionary
            methodsDict.get(info.getAnnotation().annotationType()).add(info.method);
        }
        //Sort all of the OOPTest annotated methods according to the user's given order
        methodsDict.put(OOPTest.class,
                sortOOPTests(methodsDict.get(OOPTest.class), c, tag));
        return methodsDict;
    }

    /**
     * Simple record type that serves as wrapper for a given method.
     * Contains the following information:
     *  The method itself {@link MethodInfo#method},
     *  The method's access modifier, as a string {@link MethodInfo#access},
     *  The method's name {@link MethodInfo#name},
     *  The method's OOPUnit annotation (or null if one doesn't exist) {@link MethodInfo#annotation}
     *
     * Supports basic get/set methods. MethodInfo overrides comparison: compares by name and access
     * level only. Purpose is to also consider a method equal to another method that it overrides.
     * @see MethodInfo#equals
     */
    private static class MethodInfo {
        Method method;
        String access;
        String name;
        Annotation annotation;

        private MethodInfo(Method method, String access, String name, Annotation annotation) {
            method.setAccessible(true);
            this.method = method;
            this.access = access;
            this.name = name;
            this.annotation = annotation;
        }

        public Method getMethod() {
            return method;
        }

        public String getAccess() {
            return access;
        }

        public String getName() {
            return name;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        /**
         * The comparison method for the MethodInfo type
         * @param obj: object to be compared with
         * @return 'true' iff the object is an instance of MethodInfo and:
         *  1) if obj's access level is either static or private (or both):
         *     compare by the method itself
         *  2) otherwise, compare by the methods' names
         */
        @Override
        public boolean equals(Object obj) {
            if(obj == null) {
                return false;
            }
            if(!(obj instanceof MethodInfo)) {
                return false;
            }
            MethodInfo toCompare = (MethodInfo) obj;
            if(access.contains("static") || access.contains("private")) {
                return toCompare.method.equals(method);
            }
            return name.equals(toCompare.name);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode() * 2;
            if(access.contains("private") || access.contains("static")) {
                result+=1;
            }
            return result;
        }

        @Override
        public String toString() {
            return (access + " void " + name + "()");
        }
    }
}
