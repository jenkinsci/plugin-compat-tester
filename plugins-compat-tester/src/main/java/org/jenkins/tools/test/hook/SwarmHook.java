package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.model.PomData;

public class SwarmHook extends AbstractMultiParentHook {

    private static final Logger LOGGER = Logger.getLogger(SwarmHook.class.getName());

    @Override
    protected String getParentFolder() {
        return "swarm";
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
        return isSwarmPlugin(info);
    }

    private boolean isSwarmPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isSwarmPlugin(data);
    }

    private boolean isSwarmPlugin(PomData data) {
        if (data.parent != null) {
            // Non-incrementals
            return data.parent.artifactId.equalsIgnoreCase("swarm-plugin");
        } else if (!"swarm".equalsIgnoreCase(data.artifactId)) {
            return false;
        } else {
            LOGGER.log(
                    Level.WARNING,
                    "Swarm Plugin may have been incrementalified. "
                            + "See JENKINS-55169 and linked tickets. Will guess by the packaging: {0}",
                    data.getPackaging());
            return "hpi".equalsIgnoreCase(data.getPackaging());
        }
    }
}
