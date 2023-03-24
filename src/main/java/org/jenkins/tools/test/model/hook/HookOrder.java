/** */
package org.jenkins.tools.test.model.hook;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
/** Relative order to sort hooks in. The higher the number the higher priority the hook has. */
public @interface HookOrder {

    public int order() default 0;
}
