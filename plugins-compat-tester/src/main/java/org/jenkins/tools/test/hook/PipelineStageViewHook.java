package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class PipelineRestApiHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "pipeline-stage-view";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/pipeline-stage-view-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "pipeline-stage-view";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin){
        return (currentPlugin.getDisplayName() == "pipeline-rest-api") ? "rest-api" : "ui";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isPipelineStageViewPlugin(info);
    }

    public static boolean isPipelineStageViewPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isPipelineStageViewPlugin(data);
    }

    public static boolean isPipelineStageViewPlugin(PomData data) {
        return data.groupId.equals("org.jenkins-ci.plugins.pipeline-stage-view") || data.artifactId.contains("pipeline-rest-api");
    }
}
