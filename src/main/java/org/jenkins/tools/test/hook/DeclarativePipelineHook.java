package org.jenkins.tools.test.hook;

import java.util.Map;
import java.util.Set;
import org.jenkins.tools.test.model.PomData;

/**
 * Workaround for the Pipeline: Declarative plugins since they are stored in a central repository.
 */
public class DeclarativePipelineHook extends AbstractMultiParentHook {

    private static final Set<String> ARTIFACT_IDS =
            Set.of(
                    "pipeline-model-api",
                    "pipeline-model-definition",
                    "pipeline-model-extensions",
                    "pipeline-stage-tags-metadata");

    @Override
    protected String getParentFolder() {
        return "pipeline-model-definition-plugin";
    }

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "org.jenkinsci.plugins".equals(data.groupId)
                && ARTIFACT_IDS.contains(data.artifactId)
                && "hpi".equals(data.getPackaging());
    }
}
