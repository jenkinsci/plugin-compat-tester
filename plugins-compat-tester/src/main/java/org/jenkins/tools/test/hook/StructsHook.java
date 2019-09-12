package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import org.jenkins.tools.test.model.PomData;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StructsHook extends AbstractMultiParentHook {

    private static final Logger LOGGER =
            Logger.getLogger(StructsHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "structs-plugin";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/structs-plugin.git";
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

    public static boolean isStructsPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isStructsPlugin(data);
    }

    public static boolean isStructsPlugin(PomData data) {
        if (data.parent != null) {
            // Non-incrementals
            return data.parent.artifactId.equalsIgnoreCase("structs-parent");
        } else if (!"structs".equalsIgnoreCase(data.artifactId)) {
            return false;
        } else {
            LOGGER.log(Level.WARNING, "Structs Plugin may have been incrementalified. " +
                    "See JENKINS-55169 and linked tickets. Will guess by the packaging: {0}",
                    data.getPackaging());
            return "hpi".equalsIgnoreCase(data.getPackaging());
        }
    }
}
