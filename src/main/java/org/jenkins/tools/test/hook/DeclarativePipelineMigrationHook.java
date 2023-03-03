package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;
import org.kohsuke.MetaInfServices;

/**
 * Workaround for the Declarative Pipeline Migration Assistant plugins since they are stored in a
 * central repository.
 */
@MetaInfServices(PluginCompatTesterHookBeforeCheckout.class)
public class DeclarativePipelineMigrationHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS =
            Set.of(
                    "declarative-pipeline-migration-assistant",
                    "declarative-pipeline-migration-assistant-api");

    @Override
    protected String getParentFolder() {
        return "declarative-pipeline-migration-assistant-plugin";
    }

    @Override
    public boolean check(@NonNull BeforeCheckoutContext context) {
        Model model = context.getModel();
        return "org.jenkins-ci.plugins.to-declarative".equals(model.getGroupId())
                && ARTIFACT_IDS.contains(model.getArtifactId())
                && "hpi".equals(model.getPackaging());
    }
}
