package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import org.jenkins.tools.test.model.PomData;

public class MinaSshdApi extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "mina-sshd-api-plugin";
    }

    @Override
    protected String getParentProjectName() {
        return "mina-sshd-api-parent";
    }

    @Override
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin){
        return currentPlugin.getDisplayName();
    }

    @Override
    public boolean check(Map<String, Object> info) {
        return isMinaSshdApiPlugin(info);
    }

    private boolean isMinaSshdApiPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return isMinaSshdApiPlugin(data);
    }

    private boolean isMinaSshdApiPlugin(PomData data) {
        return data.groupId.equals("io.jenkins.plugins.mina-sshd-api");
    }
}
