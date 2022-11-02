package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

public class WorkflowCpsHooks {

    public static boolean isMultiModuleVersionOfWorkflowCps(Map<String, Object> info) {
        UpdateSite.Plugin plugin = info.get("plugin") != null ? (UpdateSite.Plugin) info.get("plugin") : null;
        if (plugin != null && plugin.name.equalsIgnoreCase("workflow-cps") && plugin.version != null) {
            VersionNumber pluginVersion = new VersionNumber(plugin.version);
            VersionNumber multiModuleSince = new VersionNumber("2803.v1a_f77ffcc773");
            return pluginVersion.isNewerThan(multiModuleSince);
        }
        return false;
    }

    public static class MultiModuleHook extends AbstractMultiParentHook {
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
    }

    /**
     * Do not run tests for groovy-cps-dgm-builder and groovy-cps in the PCT.
     */
    public static class OnlyTestPluginHook extends PluginCompatTesterHookBeforeExecution {
        @Override
        public Map<String, Object> action(Map<String, Object> info) throws Exception {
            if (isMultiModuleVersionOfWorkflowCps(info)) {
                List<String> args = (List<String>) info.get("args");
                if (args != null) {
                    args.add("-pl");
                    args.add("plugin");
                }
            }
            return info;
        }
    }
}
