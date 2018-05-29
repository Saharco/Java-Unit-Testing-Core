package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should only mark a single field of type OOPExpectedException.
 * We use this annotation to mark an OOP test class's expected exception, which will store the
 * information of the expected exception to be thrown by an OOPTest annotated method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OOPExceptionRule {
}
