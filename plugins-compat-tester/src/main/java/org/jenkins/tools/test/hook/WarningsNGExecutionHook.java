package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.PomData;

import java.util.Map;

/**
 * Workaround for Warnings NG plugin since it needs execute integration tests.
 */
public class WarningsNGExecutionHook extends PluginWithIntegrationTestsHook {

    @Override
    public boolean check(Map<String, Object> info) {
        PomData data = (PomData) info.get("pomData");
        return "warnings-ng-parent".equals(data.artifactId) // localCheckoutDir
                || "warnings-ng".equals(data.artifactId); // checkout
    }

}
