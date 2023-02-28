package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

public class WorkflowCpsHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "workflow-cps-plugin";
    }

    @Override
    protected String getPluginFolderName(@NonNull BeforeCheckoutContext context) {
        return "plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        PomData data = context.getPomData();
        if (context.getPlugin() != null && context.getPlugin().version != null) {
            VersionNumber pluginVersion = new VersionNumber(context.getPlugin().version);
            // 2803 was the final release before it became a multi-module project.
            // The history of groovy-cps history was merged into the repo, so the first multi-module
            // release will be a little over 3500.
            VersionNumber multiModuleSince = new VersionNumber("3500");
            return "org.jenkins-ci.plugins.workflow".equals(data.groupId)
                    && "workflow-cps".equals(data.artifactId)
                    && pluginVersion.isNewerThan(multiModuleSince)
                    && "hpi".equals(data.getPackaging());
        }
        return false;
    }
}
