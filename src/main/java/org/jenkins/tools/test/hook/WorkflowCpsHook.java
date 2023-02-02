package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class WorkflowCpsHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "workflow-cps-plugin";
    }

    @Override
    protected String getParentProjectName() {
        return "workflow-cps";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        UpdateSite.Plugin plugin =
                info.get("plugin") != null ? (UpdateSite.Plugin) info.get("plugin") : null;
        if (plugin != null && plugin.version != null) {
            VersionNumber pluginVersion = new VersionNumber(plugin.version);
            // 2803 was the final release before it became a multi-module project.
            // The history of groovy-cps history was merged into the repo, so the first multi-module
            // release will be a little over 3500.
            VersionNumber multiModuleSince = new VersionNumber("3500");
            return "org.jenkins-ci.plugins.workflow".equals(data.groupId)
                    && "workflow-cps".equals(data.artifactId)
                    && pluginVersion.isNewerThan(multiModuleSince)
                    && "hpi".equals(data.getPackaging());
        }
        return false;
    }
}
