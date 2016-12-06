package org.jenkins.tools.test.model.hook;

import org.apache.commons.lang.ClassUtils;

import java.util.Map;

/**
 * An abstract class that marks a hook that runs before the checkout stage of the
 * Plugins Compat Tester.
 *
 * This exists simply for the ability to check when a subclass should be implemented.
 */
public abstract class PluginCompatTesterHookBeforeCheckout implements PluginCompatTesterHook {
    /**
     * Check the:
     *  + executionResult - if set, the required result of this execution (CANNOT CHECK IN THIS CLASS)
     *  + runCheckout - if the plugin should be checked out again
     *  + pluginDir - if set, the location of the plugin directory
     */
    public void validate(Map<String, Object> toCheck) throws Exception {
        if((toCheck.get("runCheckout") != null &&
            (toCheck.get("runCheckout").getClass().isPrimitive() || ClassUtils.wrapperToPrimitive(toCheck.get("runCheckout").getClass()) != null)) &&
            (toCheck.get("pluginDir") != null && 
            toCheck.get("pluginDir") instanceof String) ) {
                throw new IllegalArgumentException("A hook modified a required parameter for plugin checkout.");
        }
    } 
}