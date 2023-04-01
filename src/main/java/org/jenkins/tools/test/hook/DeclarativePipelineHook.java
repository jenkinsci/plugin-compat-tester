package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

/**
 * Workaround for the Pipeline: Declarative plugins since they are stored in a central repository.
 */
@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class DeclarativePipelineHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS = Set.of(
            "pipeline-model-api",
            "pipeline-model-definition",
            "pipeline-model-extensions",
            "pipeline-stage-tags-metadata");

    @Override
    protected String getParentFolder() {
        return "pipeline-model-definition-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "org.jenkinsci.plugins".equals(model.getGroupId())
                && ARTIFACT_IDS.contains(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
