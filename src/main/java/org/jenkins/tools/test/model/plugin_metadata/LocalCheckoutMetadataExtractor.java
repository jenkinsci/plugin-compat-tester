package org.jenkins.tools.test.model.plugin_metadata;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata.Builder;

public class LocalCheckoutMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(LocalCheckoutMetadataExtractor.class.getName());

    // artifactId\tversion\t\directory
    private static Pattern p = Pattern.compile("(?<id>[^\t]+)\t(?<version>[^\t]+)\t(?<path>[^\t]+)");

    public static List<PluginMetadata> extractMetadata(File localCheckoutDir, PluginCompatTesterConfig config)
            throws MetadataExtractionException {
        File log = new File(config.getWorkingDir(), "local-metadata.log");
        MavenRunner runner =
                new ExternalMavenRunner(config.getExternalMaven(), config.getMavenSettings(), config.getMavenArgs());
        Set<String> excludedPlugins = config.getExcludePlugins();
        Set<String> includedPlugins = config.getIncludePlugins();
        try {
            runner.run(
                    Map.of("output", log.getAbsolutePath()),
                    localCheckoutDir,
                    null,
                    null,
                    "-q",
                    // TODO to switch to release version
                    // https://github.com/jenkinsci/maven-hpi-plugin/pull/463
                    "org.jenkins-ci.tools:maven-hpi-plugin:3.42-rc1408.71cefb_fc63b_d:list-plugins",
                    "-P",
                    "consume-incrementals");

            List<String> lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            List<PluginMetadata> metadata = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank()) {
                    PluginMetadata pm = toPluginMetadata(localCheckoutDir, line.trim());
                    if (excludedPlugins != null && excludedPlugins.contains(pm.getPluginId())) {
                        LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", pm.getPluginId());
                    } else if (includedPlugins != null
                            && !includedPlugins.isEmpty()
                            && !includedPlugins.contains(pm.getPluginId())) {
                        LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", pm.getPluginId());
                    } else {
                        metadata.add(toPluginMetadata(localCheckoutDir, line.trim()));
                    }
                }
            }
            if (metadata.isEmpty()) {
                throw new MetadataExtractionException("failed to locate any plugins in local checkout");
            }
            Files.deleteIfExists(log.toPath());
            return metadata;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PomExecutionException e) {
            throw new MetadataExtractionException("failed to extract plugins from local checkout", e);
        }
    }

    /**
     * COnveret a line in the output from hpi:list-plugins to a PluginMetadata entry.
     *
     * @param cloneDirectory the directory in which to make paths relative to.
     * @throws PluginSourcesUnavailableException
     */
    private static PluginMetadata toPluginMetadata(File cloneDirectory, String hpiListEntry)
            throws MetadataExtractionException {
        Builder builder = new PluginMetadata.Builder();
        Matcher m = p.matcher(hpiListEntry);
        if (!m.matches()) {
            throw new MetadataExtractionException("Could not extract metadata from local checkout: " + hpiListEntry);
        }
        builder.withPluginId(m.group("id"));
        builder.withVersion(m.group("version"));
        builder.withGitUrl(cloneDirectory.toURI().toString());
        builder.withModulePath(relativePath(cloneDirectory, m.group("path")));

        return builder.build();
    }

    private static String relativePath(File base, String full) {
        Path rootPath = base.toPath();
        Path modulePath = Path.of(full);
        return rootPath.relativize(modulePath).toString();
    }
}
