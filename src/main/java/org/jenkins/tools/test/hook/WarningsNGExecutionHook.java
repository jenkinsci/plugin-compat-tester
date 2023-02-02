package org.jenkins.tools.test.hook;

import java.util.Map;
import org.jenkins.tools.test.model.PomData;

/** Workaround for Warnings NG plugin since it needs execute integration tests. */
public class WarningsNGExecutionHook extends PluginWithFailsafeIntegrationTestsHook {

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "warnings-ng-parent".equals(data.artifactId) // localCheckoutDir
                || "warnings-ng".equals(data.artifactId); // checkout
    }
}
