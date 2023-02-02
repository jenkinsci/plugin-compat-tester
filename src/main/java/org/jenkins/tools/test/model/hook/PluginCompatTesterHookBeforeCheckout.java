package org.jenkins.tools.test.model.hook;

import java.util.Map;
import org.apache.commons.lang.ClassUtils;

/**
 * An abstract class that marks a hook that runs before the checkout stage of the Plugin
 * Compatibility Tester.
 *
 * <p>This exists simply for the ability to check when a subclass should be implemented.
 */
public abstract class PluginCompatTesterHookBeforeCheckout implements PluginCompatTesterHook {
    /**
     * Check the value of {@code runCheckout} (if the plugin should be checked out again) and {@code
     * pluginDir} (if set, the location of the plugin directory).
     */
    @Override
    public void validate(Map<String, Object> toCheck) {
        if (toCheck.get("runCheckout") != null
                && (toCheck.get("runCheckout").getClass().isPrimitive()
                        || ClassUtils.wrapperToPrimitive(toCheck.get("runCheckout").getClass())
                                != null)
                && toCheck.get("pluginDir") != null
                && toCheck.get("pluginDir") instanceof String) {
            throw new IllegalArgumentException(
                    "A hook modified a required parameter for plugin checkout.");
        }
    }
}
