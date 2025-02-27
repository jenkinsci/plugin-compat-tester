package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
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
    public String getDefaultMinimumVersion() {
        return "2386.v82359624ea_05";
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        super.action(context);
        /*
         * The version of JUnit 5 used at runtime must match the version of JUnit 5 used to compile the tests, but the
         * inclusion of a newer test harness might cause the HPI plugin to try to use a newer version of JUnit 5 at
         * runtime to satisfy upper bounds checks, so exclude JUnit 5 from upper bounds analysis.
         */
        context.getUpperBoundsExcludes().add("org.junit.jupiter:junit-jupiter-api");
    }
}
