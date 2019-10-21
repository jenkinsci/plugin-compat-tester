package org.jenkins.tools.test.hook;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

public class TransformPom extends PluginCompatTesterHookBeforeExecution {
    private static final Logger LOGGER = Logger.getLogger(TransformPom.class.getName());
    private static final String CORE_NEW_PARENT_POM = "1.646";
    private static final String CORE_WITHOUT_WAR_FOR_TEST = "2.64";
    private static final String PLUGINS_PARENT_POM_FOR_CORE_WITHOUT_WAR_TEST = "2.33";

    private static final String NEW_PLUGINS_PARENT_VERSION_KEY = "newPluginsParentPom";

    public TransformPom() {
        System.out.println("Loaded TransformPom");
    }

    /**
     * Check if the POM should be transformed for the given plugin.
     */
    @Override
    public boolean check(Map<String, Object> info) {
        boolean mustTransformPom = false;
        // TODO future versions of DEFAULT_PARENT_GROUP/ARTIFACT may be able to use this as well
        PomData pomData = (PomData)info.get("pomData");
        MavenCoordinates parent = pomData.parent;
        MavenCoordinates coreCoordinates = (MavenCoordinates)info.get("coreCoordinates");

        final boolean isCB, parentV2, parentUnder233;
        boolean isStructs = StructsHook.isStructsPlugin(pomData);
        boolean isBO = BlueOceanHook.isBOPlugin(pomData);
        boolean isDeclarativePipeline = DeclarativePipelineHook.isDPPlugin(pomData);
        boolean isCasC = ConfigurationAsCodeHook.isCascPlugin(pomData);
        boolean isPipelineStageViewPlugin = PipelineRestApiHook.isPipelineStageViewPlugin(pomData);
        boolean isSwarm = SwarmHook.isSwarmPlugin(pomData);
        boolean pluginPOM = pomData.isPluginPOM();
        if (parent != null) {
            isCB = parent.matches("com.cloudbees.jenkins.plugins", "jenkins-plugins") ||
                    // TODO ought to analyze the chain of parent POMs, which would lead to com.cloudbees.jenkins.plugins:jenkins-plugins in this case:
                    parent.matches("com.cloudbees.operations-center.common", "operations-center-parent") ||
                    parent.matches("com.cloudbees.operations-center.client", "operations-center-parent-client");
            parentV2 = parent.compareVersionTo("2.0") >= 0;
            parentUnder233 = parentV2 && parent.compareVersionTo(PLUGINS_PARENT_POM_FOR_CORE_WITHOUT_WAR_TEST) < 0;
        } else {
            //TODO(oleg_nenashev): all these assumptions are unreliable at best (JENKINS-55169)
            LOGGER.log(Level.WARNING, "Parent POM is missing for {0}. " +
                    "Likely it is Incrementals Plugin, hence assuming it's not a CloudBees one and that the version is above 3.4 (JENKINS-55169)", pomData.artifactId);
            isCB = false;
            parentV2 = true;
            parentUnder233 = false;
        }

        boolean coreRequiresNewParentPOM = coreCoordinates.compareVersionTo(CORE_NEW_PARENT_POM) >= 0;
        boolean coreRequiresPluginOver233 = coreCoordinates.compareVersionTo(CORE_WITHOUT_WAR_FOR_TEST) >= 0;

        if (isDeclarativePipeline || isBO || isCB || isStructs || isCasC || isPipelineStageViewPlugin || isSwarm || (pluginPOM && parentV2)) {
            List<String> argsToMod = (List<String>)info.get("args");
            argsToMod.add("-Djenkins.version=" + coreCoordinates.version);
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

    @Override
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