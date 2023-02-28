package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;

/**
 * Workaround for the Declarative Pipeline Migration Assistant plugins since they are stored in a
 * central repository.
 */
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
        PomData data = context.getPomData();
        return "org.jenkins-ci.plugins.to-declarative".equals(data.groupId)
                && ARTIFACT_IDS.contains(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
