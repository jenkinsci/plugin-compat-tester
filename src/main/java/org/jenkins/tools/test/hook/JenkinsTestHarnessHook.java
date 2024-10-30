package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class JenkinsTestHarnessHook extends PropertyVersionHook {

    @Override
    public String getProperty() {
        return "jenkins-test-harness.version";
    }

    @Override
    public String getMinimumVersion() {
        return "1";
    }

}
