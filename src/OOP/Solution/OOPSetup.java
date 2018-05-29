package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should only mark methods
 * We use this annotation to mark an OOP test class's methods that should run at the start: before
 * all of the class's test methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OOPSetup {
}
