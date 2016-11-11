package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TransformPom extends PluginCompatTesterHookBeforeExecution {
    private static final String CORE_NEW_PARENT_POM = "1.646";

    public TransformPom() {
        System.out.println("Loaded TransformPom");
    }

    /**
     * Check if the pom should be transformed for the given plugin.
     */
    public boolean check(Map<String, Object> info) {
        boolean mustTransformPom = false;
        // TODO future versions of DEFAULT_PARENT_GROUP/ARTIFACT may be able to use this as well
        PomData pomData = (PomData)info.get("pomData");        
        MavenCoordinates parent = pomData.parent;
        MavenCoordinates coreCoordinates = (MavenCoordinates)info.get("coreCoordinates");
        boolean isCB = parent.matches("com.cloudbees.jenkins.plugins", "jenkins-plugins") ||
                // TODO ought to analyze the chain of parent POMs, which would lead to com.cloudbees.jenkins.plugins:jenkins-plugins in this case:
                parent.matches("com.cloudbees.operations-center.common", "operations-center-parent") ||
                parent.matches("com.cloudbees.operations-center.client", "operations-center-parent-client");
        boolean pluginPOM = parent.matches("org.jenkins-ci.plugins", "plugin");
        boolean parentV2 = parent.compareVersionTo("2.0") >= 0;
        boolean coreRequiresNewParentPOM = coreCoordinates.compareVersionTo(CORE_NEW_PARENT_POM) >= 0;

        if ( isCB || (pluginPOM && parentV2)) {
            List<String> argsToMod = (List<String>)info.get("args");
            argsToMod.add("-Djenkins.version=" + coreCoordinates.version);
            argsToMod.add("-Dhpi-plugin.version=1.117"); // TODO would ideally pick up exact version from org.jenkins-ci.main:pom
            // There are rules that avoid dependencies on a higher java level. Depending on the baselines and target cores
            // the plugin may be Java 6 and the dependencies bring Java 7
            argsToMod.add("-Denforcer.skip=true");
            info.put("args", argsToMod);
        } else if (coreRequiresNewParentPOM && pluginPOM && parentV2) {
            throw new RuntimeException("New parent POM required for core >= 1.646");
        } else {
            mustTransformPom = true;
        }

        return mustTransformPom;
    }

    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        MavenCoordinates coreCoordinates = (MavenCoordinates)moreInfo.get("coreCoordinates");
        ((MavenPom)moreInfo.get("pom")).transformPom(coreCoordinates);
        return moreInfo;
    }
}