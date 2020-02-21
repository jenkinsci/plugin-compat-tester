package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.PomData;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Workaround for the Declarative Pipeline Migration Assistant plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineMigrationHook
    extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(DeclarativePipelineMigrationHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "declarative-pipeline-migration-assistant";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/declarative-pipeline-migration-assistant-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "declarative-pipeline-migration-assistant-parent";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isPlugin(info);
    }

    public static boolean isPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isPlugin(data);
    }

    public static boolean isPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("declarative-pipeline-migration-assistant-parent");
        } else {
            LOGGER.log(Level.WARNING, "Artifact {0} has no parent POM, likely it was incrementalified (JEP-305). " +
                "Will guess the plugin by artifact ID. FTR JENKINS-55169", data.artifactId);
            return data.artifactId.contains("declarative-pipeline-migration-assistant") ||
                data.artifactId.contains("declarative-pipeline-migration-assistant-api");
        }
    }

}
