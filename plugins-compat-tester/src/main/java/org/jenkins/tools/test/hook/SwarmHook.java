package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class SwarmHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "swarm";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/swarm-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "swarm-plugin";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }

    @Override
    public boolean check(Map<String, Object> info) throws Exception {
        return isSwarmPlugin(info);
    }

    public static boolean isSwarmPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isSwarmPlugin(data);
    }

    public static boolean isSwarmPlugin(PomData data) {
        return data.parent.artifactId.equalsIgnoreCase("swarm-plugin");
    }
}
