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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata.Builder;

public class LocalCheckoutMetadataExtractor {

    // artifactId\tversion\t\directory
    private static Pattern p =
            Pattern.compile("(?<id>[^\t]+)\t(?<version>[^\t]+)\t(?<path>[^\t]+)");

    public static List<PluginMetadata> extractMetadata(
            File localCheckoutDir, PluginCompatTesterConfig config)
            throws PluginSourcesUnavailableException {
        File log = new File(config.getWorkingDir(), "local-metadata.log");
        MavenRunner runner =
                new ExternalMavenRunner(
                        config.getExternalMaven(),
                        config.getMavenSettings(),
                        config.getMavenArgs());
        try {
            runner.run(
                    Map.of("output", log.getAbsolutePath()),
                    localCheckoutDir,
                    null,
                    null,
                    "-q",
                    // TODO to switch to release version
                    // https://github.com/jenkinsci/maven-hpi-plugin/pull/463
                    "org.jenkins-ci.tools:maven-hpi-plugin:3.42-rc1408.71cefb_fc63b_d:list-plugins");

            List<String> lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
            List<PluginMetadata> metadata = new ArrayList<>();
            for (String line : lines) {
                metadata.add(toPluginMetadata(localCheckoutDir, line));
            }
            if (metadata.isEmpty()) {
                throw new PluginSourcesUnavailableException(
                        "failed to locate any plguins in local checkout");
            }
            Files.deleteIfExists(log.toPath());
            return metadata;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (PomExecutionException e) {
            throw new PluginSourcesUnavailableException(
                    "failed to extract plguins from local checkout", e);
        }
    }

    /**
     * COnveret a line in the output from hpi:list-plugins to a PluginMetadata entry.
     *
     * @param cloneDirectory the directory in which to make paths relative to.
     */
    private static PluginMetadata toPluginMetadata(File cloneDirectory, String hpiListEntry) {
        Builder builder = new PluginMetadata.Builder();
        Matcher m = p.matcher(hpiListEntry);
        builder.withPluginId(m.group("id"));
        builder.withVersion(m.group("version"));
        builder.withModulePath(relativePath(cloneDirectory, m.group("path")));
        return builder.build();
    }

    private static String relativePath(File base, String full) {
        Path rootPath = base.toPath();
        Path modulePath = Path.of(full);
        return rootPath.relativize(modulePath).toString();
    }
}
