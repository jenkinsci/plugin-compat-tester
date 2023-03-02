package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Custom execution hook for plugins whose parent is {@code org.jvnet.hudson.plugins:analysis-pom}.
 * These plugins use Maven Failsafe Plugin in their test suites.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class AnalysisPomExecutionHook extends PluginWithFailsafeIntegrationTestsHook {

    private static final Set<String> ARTIFACT_IDS =
            Set.of(
                    "analysis-model-api",
                    "bootstrap5-api",
                    "checks-api",
                    "echarts-api",
                    "font-awesome-api",
                    "forensics-api",
                    "jquery3-api",
                    "plugin-util-api",
                    "popper2-api");

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        Model model = context.getModel();
        return "io.jenkins.plugins".equals(model.getGroupId())
                && ARTIFACT_IDS.contains(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
