package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import java.util.Set;
import org.jenkins.tools.test.model.PomData;

public class PipelineStageViewHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS = Set.of(
            "pipeline-rest-api",
            "pipeline-stage-view");

    @Override
    protected String getParentFolder() {
        return "pipeline-stage-view-plugin";
    }

    @Override
    protected String getParentProjectName() {
        return "pipeline-stage-view";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin){
        return currentPlugin.getDisplayName().equals("pipeline-rest-api") ? "rest-api" : "ui";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "org.jenkins-ci.plugins.pipeline-stage-view".equals(data.groupId)
                && ARTIFACT_IDS.contains(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
