package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should only mark methods
 * We use this annotation to mark an OOP test class's test methods!
 * Annotation's {@code order()} - the ordinal order of this test, which represents when it runs
 * Annotation's {@code tag()} - the test's tag, which will be filtered out if the user does not
 * wish to run it. By default - an empty tag means that the test will always run
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OOPTest {
    int order() default 0;
    String tag() default "";
}
