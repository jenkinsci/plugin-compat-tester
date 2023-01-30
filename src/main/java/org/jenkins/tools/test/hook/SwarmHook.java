package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class SwarmHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "swarm-plugin";
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
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "org.jenkins-ci.plugins".equals(data.groupId)
                && "swarm".equals(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
