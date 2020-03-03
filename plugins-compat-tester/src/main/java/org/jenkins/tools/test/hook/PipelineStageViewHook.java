package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkins.tools.test.model.PomData;

public class PipelineStageViewHook
    extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger( PipelineStageViewHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "pipeline-stage-view";
    }

    @Override
    protected String getPluginFolderName( UpdateSite.Plugin currentPlugin ) {
        return "ui";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/pipeline-stage-view-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "parent-pom";
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
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("parent-pom");
        } else {
            LOGGER.log( Level.WARNING, "Artifact {0} has no parent POM, likely it was incrementalified (JEP-305). " +
                "Will guess the plugin by artifact ID. FTR JENKINS-55169", data.artifactId);
            return data.artifactId.contains("pipeline-rest-api") ||
                data.artifactId.contains("pipeline-stage-view");
        }
    }
}
