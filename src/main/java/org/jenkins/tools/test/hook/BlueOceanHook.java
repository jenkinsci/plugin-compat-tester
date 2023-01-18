package org.jenkins.tools.test.hook;

import java.util.Map;
import org.jenkins.tools.test.model.PomData;

/**
 * Workaround for the Blue Ocean plugins since they are
 * stored in a central repository.
 */
public class BlueOceanHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "blueocean-plugin";
    }


    @Override
    protected String getParentProjectName() {
        return "blueocean-parent";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "io.jenkins.blueocean".equals(data.groupId)
                && (data.artifactId.startsWith("blueocean") || "jenkins-design-language".equals(data.artifactId))
                && "hpi".equals(data.getPackaging());
    }
}
