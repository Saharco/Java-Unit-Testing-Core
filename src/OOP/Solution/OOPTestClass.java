package OOP.Solution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OOPTestClass {

    public enum OOPTestClassType {
        ORDERED, UNORDERED
    }

    OOPTestClassType value() default OOPTestClassType.UNORDERED;
}