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
        PomData data = (PomData) info.get("pomData");
        return "io.jenkins".equals(data.groupId)
                && "configuration-as-code".equals(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return "plugin";
    }
}
