package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;

/** Workaround for Warnings NG plugin since it needs execute integration tests. */
public class WarningsNGExecutionHook extends PluginWithFailsafeIntegrationTestsHook {

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        PomData data = context.getPomData();
        return "warnings-ng-parent".equals(data.artifactId) // localCheckoutDir
                || "warnings-ng".equals(data.artifactId); // checkout
    }
}
