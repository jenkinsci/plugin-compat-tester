package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

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
        PomData data = context.getPomData();
        return "org.jenkins-ci.plugins.pipeline-stage-view".equals(data.groupId)
                && ARTIFACT_IDS.contains(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
