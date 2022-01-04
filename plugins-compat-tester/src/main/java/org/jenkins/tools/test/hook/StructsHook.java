package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.model.PomData;

public class StructsHook extends AbstractMultiParentHook {

    private static final Logger LOGGER =
            Logger.getLogger(StructsHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "structs-plugin";
    }


    @Override
    protected String getParentProjectName() {
        return "structs-parent";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isStructsPlugin(info);
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }

    private boolean isStructsPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        UpdateSite.Plugin plugin = moreInfo.get("plugin") != null ? (UpdateSite.Plugin) moreInfo.get("plugin") : null;
        if (plugin != null && plugin.version != null) {
            return isStructsPlugin(data, plugin.version);
        }
        return false;
    }

    private boolean isStructsPlugin(PomData data, String version) {
        if (data.artifactId.equalsIgnoreCase("structs")) {
            VersionNumber pluginVersion = new VersionNumber(version);
            VersionNumber flattenedSince = new VersionNumber("1.25");
            return pluginVersion.isOlderThan(oldVersion);
        }
        return false;
    }
}
