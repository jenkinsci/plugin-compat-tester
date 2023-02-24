package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;

/**
 * The hook interface for creating custom hooks at different points in Plugin Compatibility Tester.
 *
 * <p>Hooks can be called at various points within the process, enabling per-plugin or all-plugin
 * customization. This includes POM transformations, adding Maven arguments, and other actions that
 * enable Plugin Compatibility Tester to actually go about testing the plugin rather than throwing
 * up its hands in defeat.
 */
public abstract class PluginCompatTesterHook<C extends StageContext> {

    /**
     * Check if the plugin should be affected by this hook. There are several different ways this
     * could be implemented, and the details are left up to the user.
     *
     * <p>Always run this hook unless otherwise specified.
     */
    public boolean check(@NonNull C context) {
        return true;
    }

    /**
     * The core action of what actually needs to be done by the hook. This can do a number of things
     * such as transform the POM, return custom Maven arguments, etc.
     *
     * <p>Certain implementations could throw exceptions.
     */
    public abstract void action(@NonNull C context) throws PluginCompatibilityTesterException;
}
