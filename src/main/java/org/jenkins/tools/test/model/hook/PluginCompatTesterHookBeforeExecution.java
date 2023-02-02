package org.jenkins.tools.test.model.hook;

import java.util.Map;
import org.jenkins.tools.test.model.PomData;

/**
 * An abstract class that marks a hook that runs before the execution stage of the Plugin
 * Compatibility Tester.
 *
 * <p>This exists simply for the ability to check when a subclass should be implemented.
 */
public abstract class PluginCompatTesterHookBeforeExecution implements PluginCompatTesterHook {
    /**
     * Check the value of {@code args} (the arguments with which to run {@code mvn test}) and {@code
     * pomData} (if the plugin should be checked out again).
     */
    @Override
    public void validate(Map<String, Object> toCheck) {
        if ((toCheck.get("args") != null && toCheck.get("args") instanceof String)
                && (toCheck.get("pomData") != null && toCheck.get("pomData") instanceof PomData)) {
            throw new IllegalArgumentException(
                    "A hook modified a required parameter for plugin test execution.");
        }
    }
}
