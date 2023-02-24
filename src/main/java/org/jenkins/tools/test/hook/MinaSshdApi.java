package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class MinaSshdApi extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "mina-sshd-api-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return "io.jenkins.plugins.mina-sshd-api".equals(data.groupId)
                && data.artifactId.startsWith("mina-sshd-api")
                && "hpi".equals(data.getPackaging());
    }
}
