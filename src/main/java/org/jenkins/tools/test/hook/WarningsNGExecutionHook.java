package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/** Workaround for Warnings NG plugin since it needs execute integration tests. */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class WarningsNGExecutionHook extends PluginWithFailsafeIntegrationTestsHook {

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        Model model = context.getModel();
        return "io.jenkins.plugins".equals(model.getGroupId())
                && "warnings-ng-parent".equals(model.getArtifactId())
                && "pom".equals(model.getPackaging());
    }
}
