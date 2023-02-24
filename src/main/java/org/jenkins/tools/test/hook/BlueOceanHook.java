package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

/** Workaround for the Blue Ocean plugins since they are stored in a central repository. */
public class BlueOceanHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "blueocean-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return "io.jenkins.blueocean".equals(data.groupId)
                && (data.artifactId.startsWith("blueocean")
                        || "jenkins-design-language".equals(data.artifactId))
                && "hpi".equals(data.getPackaging());
    }
}
