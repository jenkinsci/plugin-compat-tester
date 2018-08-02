package org.jenkins.tools.test.hook;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;

public class TransformPom extends PluginCompatTesterHookBeforeExecution {
    private static final String CORE_NEW_PARENT_POM = "1.646";
    private static final String CORE_WITHOUT_WAR_FOR_TEST = "2.64";
    private static final String PLUGINS_PARENT_POM_FOR_CORE_WITHOUT_WAR_TEST = "2.33";

    private static final String NEW_PLUGINS_PARENT_VERSION_KEY = "newPluginsParentPom";

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
        @CheckForNull  MavenCoordinates warCoordinates = (MavenCoordinates)info.get("warCoordinates");
        boolean isDeclarativePipeline = parent.matches("org.jenkinsci.plugins", "pipeline-model-parent");
        boolean isCB = parent.matches("com.cloudbees.jenkins.plugins", "jenkins-plugins") ||
                // TODO ought to analyze the chain of parent POMs, which would lead to com.cloudbees.jenkins.plugins:jenkins-plugins in this case:
                parent.matches("com.cloudbees.operations-center.common", "operations-center-parent") ||
                parent.matches("com.cloudbees.operations-center.client", "operations-center-parent-client");
        boolean isBO = parent.matches("io.jenkins.blueocean", "blueocean-parent");
        boolean isStructs = parent.matches("org.jenkins-ci.plugins", "structs-parent");
        boolean pluginPOM = parent.matches("org.jenkins-ci.plugins", "plugin");
        boolean parentV2 = parent.compareVersionTo("2.0") >= 0;
        boolean parentUnder233 = parentV2 && parent.compareVersionTo(PLUGINS_PARENT_POM_FOR_CORE_WITHOUT_WAR_TEST) < 0;
        boolean coreRequiresNewParentPOM = coreCoordinates.compareVersionTo(CORE_NEW_PARENT_POM) >= 0;
        boolean coreRequiresPluginOver233 = coreCoordinates.compareVersionTo(CORE_WITHOUT_WAR_FOR_TEST) >= 0;

        if (isDeclarativePipeline || isBO || isCB || isStructs || (pluginPOM && parentV2)) {
            List<String> argsToMod = (List<String>)info.get("args");
            if (warCoordinates == null) {
                argsToMod.add("-Djenkins.version=" + coreCoordinates.version);
            } else {
                argsToMod.add("-Djenkins-core.version=" + coreCoordinates.version);
                argsToMod.add("-Djenkins-war.version=" + warCoordinates.version);
            }
            // There are rules that avoid dependencies on a higher java level. Depending on the baselines and target cores
            // the plugin may be Java 6 and the dependencies bring Java 7
            argsToMod.add("-Denforcer.skip=true");
            info.put("args", argsToMod);

            if (coreRequiresPluginOver233 && pluginPOM && parentUnder233) {
                info.put(NEW_PLUGINS_PARENT_VERSION_KEY, PLUGINS_PARENT_POM_FOR_CORE_WITHOUT_WAR_TEST);
                mustTransformPom = true;
            }
        } else if (coreRequiresNewParentPOM && pluginPOM && !parentV2) {
            throw new RuntimeException("New parent POM required for core >= 1.646");
        } else {
            mustTransformPom = true;
        }

        return mustTransformPom;
    }

    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        MavenCoordinates coreCoordinates = (MavenCoordinates)moreInfo.get("coreCoordinates");

        final String pluginsParentVersion = (String) moreInfo.get(NEW_PLUGINS_PARENT_VERSION_KEY);
        if (pluginsParentVersion != null) {
            coreCoordinates = new MavenCoordinates(coreCoordinates.groupId, coreCoordinates.artifactId, pluginsParentVersion);
        }

        ((MavenPom)moreInfo.get("pom")).transformPom(coreCoordinates);
        return moreInfo;
    }
}