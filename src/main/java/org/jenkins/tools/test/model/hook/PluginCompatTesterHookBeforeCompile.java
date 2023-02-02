package org.jenkins.tools.test.model.hook;

/**
 * An abstract class that marks a hook that runs before the compilation stage of Plugin
 * Compatibility Tester.
 *
 * <p>This exists simply for the ability to check when a subclass should be implemented.
 *
 * <p>Hooks of this type can be used to implement custom compile configurations that override the
 * one done by Plugin Compatibility Tester.
 *
 * <p>To do that, add a {@code ranCompile} property to the returned map with a true value, and make
 * sure the hook runs the compilation phase. As hooks are not executed in any particular order, any
 * hook that performs the compilation must check before if it has already been performed by another
 * hook and decide on the consequences.
 */
public abstract class PluginCompatTesterHookBeforeCompile implements PluginCompatTesterHook {

    public static final String OVERRIDE_DEFAULT_COMPILE = "ranCompile";
}
