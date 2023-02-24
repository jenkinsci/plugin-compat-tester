package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class SwarmHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "swarm-plugin";
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return "org.jenkins-ci.plugins".equals(data.groupId)
                && "swarm".equals(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
