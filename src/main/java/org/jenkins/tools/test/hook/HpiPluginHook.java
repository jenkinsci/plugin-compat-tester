package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Picking up mojo fixes not necessarily in existing plugin releases.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class HpiPluginHook extends PropertyVersionHook {

    @Override
    public String getProperty() {
        return "hpi-plugin.version";
    }

    @Override
    public String getMinimumVersion() {
        return "3.60-20241213.184504-1";
    }
}
