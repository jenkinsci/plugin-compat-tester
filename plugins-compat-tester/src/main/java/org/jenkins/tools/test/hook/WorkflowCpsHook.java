package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import java.util.Map;

public class WorkflowCpsHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "workflow-cps-plugin";
    }

    @Override
    protected String getParentProjectName() {
        return "workflow-cps-parent";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin){
        return "plugin";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isMultiModuleVersionOfWorkflowCps(info);
    }

    public static boolean isMultiModuleVersionOfWorkflowCps(Map<String, Object> info) {
        UpdateSite.Plugin plugin = info.get("plugin") != null ? (UpdateSite.Plugin) info.get("plugin") : null;
        if (plugin != null && plugin.name.equalsIgnoreCase("workflow-cps") && plugin.version != null) {
            VersionNumber pluginVersion = new VersionNumber(plugin.version);
            // 2803 was the final release before it became a multi-module project.
            // The history of groovy-cps history was merged into the repo, so the first multi-module release will be a little over 3500.
            VersionNumber multiModuleSince = new VersionNumber("3500");
            return pluginVersion.isNewerThan(multiModuleSince);
        }
        return false;
    }
}
