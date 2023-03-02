package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
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
        Model model = context.getModel();
        return "org.jenkins-ci.plugins".equals(model.getGroupId())
                && "swarm".equals(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
