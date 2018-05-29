package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should only mark methods
 * We use this annotation to mark an OOP test class's methods that should run after a test method.
 * Annotation's {@code value()} - stores an array of test methods that this method should run after
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OOPAfter {
    String[] value();
}
