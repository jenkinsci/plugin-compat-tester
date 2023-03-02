package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class ConfigurationAsCodeHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "configuration-as-code-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "io.jenkins".equals(model.getGroupId())
                && "configuration-as-code".equals(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }
}
