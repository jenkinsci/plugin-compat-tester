package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.util.Map;
import java.util.Set;
import org.jenkins.tools.test.model.PomData;

/**
 * Workaround for the Declarative Pipeline Migration Assistant plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineMigrationHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS = Set.of(
            "declarative-pipeline-migration-assistant",
            "declarative-pipeline-migration-assistant-api");

    @Override
    protected String getParentFolder() {
        return "declarative-pipeline-migration-assistant-plugin";
    }

    @Override
    protected String getParentProjectName() {
        return "declarative-pipeline-migration-assistant";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "org.jenkins-ci.plugins.to-declarative".equals(data.groupId)
                && ARTIFACT_IDS.contains(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
