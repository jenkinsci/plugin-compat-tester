package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

/** Workaround for the Blue Ocean plugins since they are stored in a central repository. */
public class BlueOceanHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "blueocean-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "io.jenkins.blueocean".equals(model.getGroupId())
                && (model.getArtifactId().startsWith("blueocean")
                        || "jenkins-design-language".equals(model.getArtifactId()))
                && "hpi".equals(model.getPackaging());
    }
}
