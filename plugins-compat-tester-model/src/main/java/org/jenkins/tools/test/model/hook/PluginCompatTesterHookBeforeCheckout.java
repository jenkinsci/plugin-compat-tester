package org.jenkins.tools.test.model.hook;

import hudson.model.UpdateSite.Plugin;
//import org.jenkins.tools.test.model.TestExecutionResult;

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
     *  + executionResult - if set, the required result of this execution
     *  + runCheckout - if the plugin should be checked out again
     *  + pluginDir - if set, the location of the plugin directory
     */
    public void validate(Map<String, Object> toCheck) throws Exception {
        /*if((toCheck.get("executionResult") != null && 
            toCheck.get("executionResult") instanceof TestExecutionResult) &&
            (toCheck.get("runCheckout") != null &&
            toCheck.get("runCheckout")) &&
            (toCheck.get("pluginDir") != null && 
            toCheck.get("pluginDir") instanceof String) ) {
                throw new IllegalArgumentException("A hook modified a required parameter for plugin checkout.");
        }*/
    } 
}