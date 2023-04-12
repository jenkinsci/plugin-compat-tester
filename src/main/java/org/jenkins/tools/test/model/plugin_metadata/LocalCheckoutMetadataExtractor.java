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
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin.Builder;

public class LocalCheckoutMetadataExtractor {

    private static final Logger LOGGER = Logger.getLogger(LocalCheckoutMetadataExtractor.class.getName());

    // artifactId\tversion\t\directory
    private static Pattern p = Pattern.compile("(?<id>[^\t]+)\t(?<version>[^\t]+)\t(?<path>[^\t]+)");

    public static List<Plugin> extractMetadata(File localCheckoutDir, PluginCompatTesterConfig config)
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
                    // TODO only upgrade to 3.42 if necessary
                    "org.jenkins-ci.tools:maven-hpi-plugin:3.42:list-plugins",
                    "-P",
                    "consume-incrementals");

            List<String> lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            List<Plugin> plugins = new ArrayList<>();
            for (String line : lines) {
                if (!line.isBlank()) {
                    Plugin plugin = toPlugin(localCheckoutDir, line.trim());
                    if (excludedPlugins != null && excludedPlugins.contains(plugin.getPluginId())) {
                        LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", plugin.getPluginId());
                    } else if (includedPlugins != null
                            && !includedPlugins.isEmpty()
                            && !includedPlugins.contains(plugin.getPluginId())) {
                        LOGGER.log(Level.INFO, "Plugin {0} not in included plugins; skipping", plugin.getPluginId());
                    } else {
                        plugins.add(toPlugin(localCheckoutDir, line.trim()));
                    }
                }
            }
            if (plugins.isEmpty()) {
                throw new MetadataExtractionException("Failed to locate any plugins in local checkout");
            }
            Files.deleteIfExists(log.toPath());
            return plugins;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PomExecutionException e) {
            throw new MetadataExtractionException("Failed to extract plugins from local checkout", e);
        }
    }

    /**
     * Convert a line in the output from {@code hpi:list-plugins} to a {@link Plugin} entry.
     *
     * @param cloneDirectory the directory in which to make paths relative to.
     */
    private static Plugin toPlugin(File cloneDirectory, String hpiListEntry) throws MetadataExtractionException {
        Builder builder = new Plugin.Builder();
        Matcher m = p.matcher(hpiListEntry);
        if (!m.matches()) {
            throw new MetadataExtractionException("Could not extract metadata from local checkout: " + hpiListEntry);
        }
        builder.withPluginId(m.group("id"));
        builder.withVersion(m.group("version"));
        builder.withGitUrl(cloneDirectory.toURI().toString());
        builder.withModule(relativePath(cloneDirectory, m.group("path")));

        return builder.build();
    }

    private static String relativePath(File base, String full) {
        Path rootPath = base.toPath();
        Path modulePath = Path.of(full);
        return rootPath.relativize(modulePath).toString();
    }
}
