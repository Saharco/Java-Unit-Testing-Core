package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

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
            //Given class is either not a class, or not a test class
            throw new IllegalArgumentException();
        }

        //The result map of all the tests
        Map<String,OOPResult> OOPTestsResults = new HashMap<>();

        //The methods map, which maps a list of OOP annotated methods to each annotation type
        Map<Class <? extends Annotation>, List<Method>> annotatedMethods =
                getOOPMethods(testClass, tag);

        //The rules list
        List<Field> classExceptionRules = getOOPExceptionRules(testClass);

        //A copy of the given class object: initialized with the given class's 0-args constructor
        Object copyObject = initCopy(testClass);

        //Sort all of the OOPTest annotated methods according to the user's given order
        annotatedMethods.put(OOPTest.class,
                sortOOPTests(annotatedMethods.get(OOPTest.class), testClass, tag));

        //Run all of the OOPSetup annotated methods, excluding overridden methods
        callSetupMethods(annotatedMethods, copyObject);

        callTestMethods(annotatedMethods, classExceptionRules, (Class<?>)copyObject,
                OOPTestsResults);

        return new OOPTestSummary(OOPTestsResults);
    }

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

    private static void callTestMethods(Map<Class<? extends Annotation>, List<Method>>
                                      annotatedMethods, List<Field> classExceptionRules,
                                      Class<?> copyObject, Map<String,OOPResult> OOPTestsResults) {
        Object backupObject = backup(copyObject);
        for(Method test : annotatedMethods.get(OOPTest.class)) {
            //Run OOPBefore methods:
            try {
                shouldAbortCallBeforeAfter(annotatedMethods, copyObject, OOPBefore.class, test);
            } catch (Exception e) {
               /*
                * The test has failed: couldn't run OOPBefore methods.
                * Mark the test's failure, restore the object, and continue to the next test
                */
                OOPTestsResults.put(test.getName(), new
                        OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getMessage()));
                copyObjectFields(copyObject, backupObject);
                continue;
            }
            //Run Tests:
            try {
                test.invoke(copyObject);
                //The test succeeded. We mark this as a success, but will override in case of failure in OOPAfter methods
                OOPTestsResults.put(test.getName(), new OOPResultImpl(
                        OOPResult.OOPTestResult.SUCCESS, null));
            } catch(OOPAssertionFailure e) {
                OOPResult testResult = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE,
                        e.getMessage());
                OOPTestsResults.put(test.getName(), testResult);
                copyObjectFields(copyObject, backupObject);
            } catch(Exception e) {
                if(classExceptionRules.contains(OOPExpectedExceptionImpl.none())) {
                    //Unexpected exception occurred: Error!
                    OOPTestsResults.put(test.getName(), new
                            OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getMessage()));
                    copyObjectFields(copyObject, backupObject);
                } else if(expectedException(classExceptionRules, e, copyObject)) {
                    //Expected exception: Success!
                    OOPTestsResults.put(test.getName(), new OOPResultImpl(
                            OOPResult.OOPTestResult.SUCCESS, null));
                }
                else {
                    //Expected exception mismatch!
                    OOPTestsResults.put(test.getName(),new
                            OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH,
                            e.getMessage()));
                    copyObjectFields(copyObject, backupObject);
                }
            }

            //Run OOPAfter methods:
            try {
                shouldAbortCallBeforeAfter(annotatedMethods, copyObject, OOPAfter.class, test);
            } catch (Exception e) {
                /*
                 * The test has failed: couldn't run OOPAfter methods.
                 * Mark the test's failure, restore the object, and continue to the next test
                 */
                OOPTestsResults.put(test.getName(), new OOPResultImpl(OOPResult.OOPTestResult.ERROR,
                        e.getClass().getName())); //This will override the result
                copyObjectFields(copyObject, backupObject);
            }
        }
    }

    private static boolean expectedException(List<Field> classExceptionRules, Exception exception,
                                             Class<?> copyObject) {
        try {
            for(Field rule : classExceptionRules) {
                if(((OOPExpectedException)rule.get(copyObject)).assertExpected(exception)) {
                    return true;
                }
            }
        } catch(Exception e) {
            //We shouldn't get here
        }
        return false;
    }

    private static void shouldAbortCallBeforeAfter(Map<Class<? extends Annotation>, List<Method>>
                                        annotatedMethods, Class<?> copyObject,
                                        Class<? extends Annotation> annotation,
                                        Method test) throws Exception {
        Method[] methods = new Method[annotatedMethods.get(annotation).size()];
        methods = annotatedMethods.get(annotation).toArray(methods);
        if(annotation.getName().equals("OOPAfter")) {
            reverseArray(methods);
        }
        Class<?> testClass = test.getDeclaringClass();
        List<Method> suitableMethods = Arrays.stream(methods)
                .filter(m -> (containsTest(m, annotation, test)) &&
                        (m.getDeclaringClass().isAssignableFrom(testClass))) //TODO: Check if this is OK
                .collect(Collectors.toList());
        for(Method m : suitableMethods) {
//            backupObject = backup(copyObject);
            m.invoke(copyObject);
        }
    }

    private static boolean containsTest(Method m, Class<? extends Annotation> annotation,
                                        Method test) {
        return (annotation.getName().equals("OOPBefore")) ?
                Arrays.asList(m.getDeclaredAnnotation(OOPBefore.class).value())
                        .contains(test.getName()) :
                Arrays.asList(m.getDeclaredAnnotation(OOPAfter.class).value())
                        .contains(test.getName());
    }

    private static<T> void reverseArray(T[] arr) {
        List<T> orderList = Arrays.asList(arr);
        Collections.reverse(orderList);
        orderList.toArray(arr);
    }

    private static Object backup(Class<?> copyObject) {
        Object backupObject = null;
        try {
            backupObject = initCopy(copyObject);
            copyObjectFields(backupObject, copyObject);
        } catch (Exception e) {
            //We shouldn't get here
        }
        return backupObject;
    }

    private static void copyObjectFields(Object target, Object source) {
        try {
            for (Field field : source.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(target, fieldBackup(field.get(source)));
            }
        } catch (Exception e) {
            //We shouldn't get here
        }
    }

    private static Object fieldBackup(Object field) {
        Class<?> c = field.getClass();

        //Check if the object supports cloning:
        if(Cloneable.class.isAssignableFrom(c)) {
            try {
                Method cloneMethod = c.getDeclaredMethod("clone");
                cloneMethod.setAccessible(true);
                if(cloneMethod.getReturnType().equals(c) && cloneMethod.getParameterCount() == 0) {
                    return cloneMethod.invoke(field);
                }
            } catch (Exception e) {
                //The object does not support cloning
            }
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
                        }
                    });
        } catch (Exception e) {
            //We shouldn't get here
        }
    }

    private static Object initCopy(Class<?> testClass) {
        try {
            Class<?> c = Class.forName(testClass.getName());
            Constructor ctor = c.getDeclaredConstructor(); //We expect a 0-arguments constructor
            ctor.setAccessible(true); //Constructor might not be accessible
            return ctor.newInstance();
        } catch (Exception e) {
            //We shouldn't get here
        }
        return null;
    }

    private static Annotation getOOPUnitAnnotation(Method m) {
        //List of all the currently supported OOP annotations' names in the OOPUnit framework
        List<String> OOPUnitAnnotationsList = new LinkedList<>
                (Arrays.asList("OOPSetup", "OOPBefore", "OOPTest", "OOPAfter"));
        for (Annotation annotation : m.getDeclaredAnnotations()) {
            if(OOPUnitAnnotationsList.contains(annotation.annotationType().getName())) {
                //Found an annotation an OOPUnit annotation for this method
                return annotation;
            }
        }
        //The method is not part of the OOPUnit framework
        return null;
    }

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
     *  The method's access modifier {@link MethodInfo#access},
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
