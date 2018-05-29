package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should only mark classes.
 * We use this annotation to mark an OOP tests' class, which will contain test methods.
 * Annotation's {@code value()} - represents whether the class's test methods should be run in
 * order or not. By default - the tests do not run in a given order
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OOPTestClass {

    public enum OOPTestClassType {
        ORDERED, UNORDERED
    }

    OOPTestClassType value() default OOPTestClassType.UNORDERED;
}