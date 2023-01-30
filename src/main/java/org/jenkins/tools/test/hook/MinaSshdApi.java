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
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "io.jenkins.plugins.mina-sshd-api".equals(data.groupId)
                && data.artifactId.startsWith("mina-sshd-api")
                && "hpi".equals(data.getPackaging());
    }
}
