package org.jenkins.tools.test.model.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Relative order to sort hooks in.
 * The higher the number the higher priority the hook has.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HookOrder {

    int order() default 0;
}
