package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class PipelineStageViewHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS =
            Set.of("pipeline-rest-api", "pipeline-stage-view");

    @Override
    protected String getParentFolder() {
        return "pipeline-stage-view-plugin";
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return context.getPlugin().name.equals("pipeline-rest-api") ? "rest-api" : "ui";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "org.jenkins-ci.plugins.pipeline-stage-view".equals(model.getGroupId())
                && ARTIFACT_IDS.contains(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
