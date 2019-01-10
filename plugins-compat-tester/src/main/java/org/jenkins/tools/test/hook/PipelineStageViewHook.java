package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import org.jenkins.tools.test.model.PomData;

import java.util.Map;

public class PipelineStageViewHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "pipeline-stage-view-plugin";
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
    public boolean check(Map<String, Object> info) {
        return isPipelineStageViewPlugin(info);
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        if ("pipeline-rest-api".equalsIgnoreCase(currentPlugin.name)) {
            return "rest-api";
        } else if("pipeline-stage-view".equals(currentPlugin.name)) {
            return "ui";
        }
        return super.getPluginFolderName(currentPlugin);
    }

    static boolean isPipelineStageViewPlugin(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return isPipelineStageViewPlugin(data);
    }

    private static boolean isPipelineStageViewPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("parent-pom")
                  && data.parent.groupId.equalsIgnoreCase("org.jenkins-ci.plugins.pipeline-stage-view");
        } else {
            return false;
        }
    }
}
