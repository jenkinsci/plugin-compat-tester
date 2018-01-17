package org.jenkins.tools.test.hook;


import org.jenkins.tools.test.model.PomData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Workaround for the declarative pipeline plugins since they are
 * stored in a central repository.
 */
public class DeclarativePipelineHook extends AbstractMultiParentHook {

    @Override
    protected String getParentFolder() {
        return "pipeline-model-definition";
    }

    @Override
    protected String getParentUrl() {
        return "scm:git:git://github.com/jenkinsci/pipeline-model-definition-plugin.git";
    }

    @Override
    protected String getParentProjectName() {
        return "pipeline-model-definition";
    }

    @Override
    public boolean check(Map<String, Object> info) throws Exception {
        return isDPPlugin(info);
    }

    public static boolean isDPPlugin(Map<String, Object> moreInfo) {
        PomData data = (PomData) moreInfo.get("pomData");
        return data.parent.artifactId.equalsIgnoreCase("pipeline-model-parent");
    }
}
