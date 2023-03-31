package org.jenkins.tools.test.model.plugin_metadata;

import java.util.Optional;
import java.util.jar.Manifest;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.model.hook.HookOrder;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginMetadataExtractor.class)
@HookOrder(order = -1000)
public class LegacyPluginMetadateExtractor extends PluginMetadataExtractor {

    @Override
    public Optional<PluginMetadata> extractMetadata(String pluginId, Manifest manifest, Model model)
            throws PluginSourcesUnavailableException {
        // any multimodule project must have been handled before now (either the modern hook or a
        // specific hook for a legacy multi module project)
        String scm = model.getScm().getConnection();
        if (scm.startsWith("scm:git:")) {
            scm = scm.substring(8);
        } else {
            throw new PluginSourcesUnavailableException(
                    "SCM URL " + scm + " is not supported by the pct - only git urls are allowed");
        }
        assert pluginId.equals(model.getArtifactId());
        return Optional.of(
                new PluginMetadata.Builder()
                        .withPluginId(model.getArtifactId())
                        .withName(model.getName())
                        .withScmUrl(scm)
                        .withGitCommit(model.getScm().getTag())
                        .withModulePath(
                                null) // any multi module projects have already been handled by now
                        // or require new hooks.
                        .withVersion(model.getVersion())
                        .build());
    }
}
