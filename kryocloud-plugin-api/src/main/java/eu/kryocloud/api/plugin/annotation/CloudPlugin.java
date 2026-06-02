package eu.kryocloud.api.plugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CloudPlugin {

    String id();

    String name();

    String version();

    String description() default "";

    String[] authors() default {};

    String[] dependencies() default {};

    String[] softDependencies() default {};

}
