package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class ConfigurationAsCodeHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "configuration-as-code-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        return "io.jenkins".equals(data.groupId)
                && "configuration-as-code".equals(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }
}
