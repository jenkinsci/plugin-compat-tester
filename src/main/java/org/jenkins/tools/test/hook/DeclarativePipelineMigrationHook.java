package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.PomData;
import hudson.model.UpdateSite;

import java.util.Map;

/**
 * Workaround for the Declarative Pipeline Migration Assistant plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineMigrationHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "declarative-pipeline-migration-assistant";
    }


    @Override
    protected String getParentProjectName() {
        return "declarative-pipeline-migration-assistant";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin){
        return currentPlugin.getDisplayName();
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isPlugin(info);
    }

    private boolean isPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isPlugin(data);
    }

    private boolean isPlugin(PomData data) {
        if (data.parent != null) {
            return data.parent.artifactId.equalsIgnoreCase("declarative-pipeline-migration-assistant-parent");
        }
        return data.artifactId.contains("declarative-pipeline-migration-assistant") ||
            data.artifactId.contains("declarative-pipeline-migration-assistant-api");
    }

}
