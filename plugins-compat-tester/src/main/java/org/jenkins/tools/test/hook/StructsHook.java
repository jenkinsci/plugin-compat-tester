package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import org.jenkins.tools.test.model.PomData;

import java.util.Map;

public class StructsHook extends AbstractMultiParentHook {

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
    public boolean check(Map<String, Object> info) throws Exception {
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
            throw new IllegalStateException("Structs Plugin may have been incrementalified, and PCT does not supports the discovery now. " +
                    "See JENKINS-55169 and linked tickets");
        }
    }
}
