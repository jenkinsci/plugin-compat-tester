package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = 1000) // just incase it ever needs to be overridden
/**
 * Extractor that obtains all the information from the plugins Manifiest file. This requires the
 * plugin was built with a version of maven-hpi-plugin with
 * https://github.com/jenkinsci/maven-hpi-plugin/pull/436
 */
public class ModernPluginMetaDataExtractor extends PluginMetadataExtractor {

    // https://github.com/jenkinsci/maven-hpi-plugin/pull/436
    private static final Attributes.Name GIT_REVISION_ATTRIBUTE =
            new Attributes.Name("Plugin-Scm-Git-Hash");
    private static final Attributes.Name GIT_SCM_URL = new Attributes.Name("Plugin-Scm-Connection");
    private static final Attributes.Name MAVEN_MODULE_LOCATION =
            new Attributes.Name("Plugin-Scm-Git-Module-Path");
    private static final Attributes.Name PLUGIN_ID = new Attributes.Name("Short-Name");
    private static final Attributes.Name PLUGIN_NAME = new Attributes.Name("Long-Name");
    private static final Attributes.Name PLUGIN_VERSION = new Attributes.Name("Plugin-Version");

    @Override
    public Optional<PluginMetadata> extractMetadata(String pluginId, Manifest manifest, Model model)
            throws PluginSourcesUnavailableException {
        // all of the information os stored in the plugins Manifest
        Attributes mainAttributes = manifest.getMainAttributes();

        assert pluginId.equals(mainAttributes.getValue(PLUGIN_ID));

        if (mainAttributes.containsKey(GIT_REVISION_ATTRIBUTE)) {
            // we are new enought to be a modern plugin
            return Optional.of(
                    new PluginMetadata.Builder()
                            .withPluginId(mainAttributes.getValue(PLUGIN_ID))
                            .withName(mainAttributes.getValue(PLUGIN_NAME))
                            .withScmUrl(mainAttributes.getValue(GIT_SCM_URL))
                            .withGitCommit(mainAttributes.getValue(GIT_REVISION_ATTRIBUTE))
                            .withModulePath(mainAttributes.getValue(MAVEN_MODULE_LOCATION))
                            .withVersion(mainAttributes.getValue(PLUGIN_VERSION))
                            .build());
        }
        return Optional.empty();
    }
}
