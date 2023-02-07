package org.jenkins.tools.test.model.hook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;

/**
 * The hook interface for creating custom hooks at different points in Plugin Compatibility Tester.
 *
 * <p>Hooks can be called at various points within the process, enabling per-plugin or all-plugin
 * customization. This includes POM transformations, adding Maven arguments, and other actions that
 * enable Plugin Compatibility Tester to actually go about testing the plugin rather than throwing
 * up its hands in defeat.
 */
public interface PluginCompatTesterHook {

    /**
     * Check if the plugin should be affected by this hook. There are several different ways this
     * could be implemented, and the details are left up to the user.
     *
     * <p>Always run this hook unless otherwise specified.
     */
    default boolean check(Map<String, Object> info) {
        return true;
    }

    /**
     * The core action of what actually needs to be done by the hook. This can do a number of things
     * such as transform the POM, return custom Maven arguments, etc.
     *
     * <p>Certain implementations could throw exceptions.
     */
    Map<String, Object> action(Map<String, Object> moreInfo)
            throws PluginCompatibilityTesterException;

    /**
     * List the plugins this hook affects. This can be a single plugin, a list of plugins, or simply
     * all plugins.
     *
     * <p>Apply this hook to all plugins unless otherwise specified.
     */
    default List<String> transformedPlugins() {
        return new ArrayList<>(Collections.singletonList("all"));
    }

    /** Check the object used for this hook. */
    void validate(Map<String, Object> toCheck);
}
