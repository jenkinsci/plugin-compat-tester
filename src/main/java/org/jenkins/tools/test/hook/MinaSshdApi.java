package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class MinaSshdApi extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "mina-sshd-api-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "io.jenkins.plugins.mina-sshd-api".equals(model.getGroupId())
                && model.getArtifactId().startsWith("mina-sshd-api")
                && "hpi".equals(model.getPackaging());
    }
}
