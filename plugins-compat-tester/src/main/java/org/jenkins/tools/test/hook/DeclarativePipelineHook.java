package org.jenkins.tools.test.hook;


import org.jenkins.tools.test.model.PomData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Workaround for the declarative pipeline plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineHook extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(DeclarativePipelineHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "pipeline-model-definition";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/pipeline-model-definition-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "pipeline-model-definition";
    }

    @Override
    public boolean check(Map<String, Object> info) throws Exception {
        return isDPPlugin(info);
    }

    public static boolean isDPPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isDPPlugin(data);
    }

    public static boolean isDPPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("pipeline-model-parent");
        } else {
            LOGGER.log(Level.WARNING, "Artifact {0} has no parent POM, likely it was incrementalified (JEP-305). " +
                    "Will guess the plugin by artifact ID. FTR JENKINS-55169", data.artifactId);
            return data.artifactId.contains("pipeline-model");
        }
    }
}
