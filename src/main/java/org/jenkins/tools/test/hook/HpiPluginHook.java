package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * The {@code overrideWar} option is available in HPI Plugin 3.29 or later, but many plugins under
 * test still use an older plugin parent POM and therefore an older HPI plugin version. As a
 * temporary workaround, we override the HPI plugin version to a recent version. When all plugins in
 * the managed set are using a plugin parent POM with HPI Plugin 3.29 or later (i.e., plugin parent
 * POM 4.44 or later), this can be deleted.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class HpiPluginHook extends PropertyVersionHook {

    @Override
    public String getProperty() {
        return "hpi-plugin.version";
    }

    @Override
    public String getMinimumVersion() {
        return "3.41-rc1404.03cea_d3a_c95e";
    }
}
