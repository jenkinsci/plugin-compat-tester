package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Recent core versions require a relatively recent Jenkins Test Harness, so ensure that a minimum
 * supported version is in place.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class JenkinsTestHarnessHook extends PropertyVersionHook {

    @Override
    public String getProperty() {
        return "jenkins-test-harness.version";
    }

    @Override
    public String getMinimumVersion() {
        return "1903.vf505ecb_63589";
    }

    @Override
    public String getMinimumPluginParentPomVersion() {
        return "4.48";
    }
}
