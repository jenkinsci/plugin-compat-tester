package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class ConfigurationAsCodeHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "configuration-as-code-plugin";
    }


    @Override
    protected String getParentProjectName() {
        return "configuration-as-code";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isCascPlugin(info);
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }

    private boolean isCascPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isCascPlugin(data);
    }

    private boolean isCascPlugin(PomData data) {
        if (data.parent != null) {
            // Non-incrementals
            return data.parent.groupId.equalsIgnoreCase("io.jenkins.configuration-as-code");
        }

        return "configuration-as-code".equalsIgnoreCase(data.artifactId);
    }
}
