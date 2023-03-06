package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/** Workaround for Warnings NG plugin since it needs execute integration tests. */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class WarningsNGExecutionHook extends PluginWithFailsafeIntegrationTestsHook {

    private static final Set<String> ARTIFACT_IDS =
            Set.of(/* localCheckoutDir */ "warnings-ng-parent", /* checkout */ "warnings-ng");

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        Model model = context.getModel();
        return "io.jenkins.plugins".equals(model.getGroupId())
                && ARTIFACT_IDS.contains(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
