package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;

/**
 * A generic class for ensuring that a property-versioned dependency is at a certain minimum
 * version, dynamically setting the property to a newer version (but only if necessary).
 */
public abstract class PropertyVersionHook extends PluginCompatTesterHookBeforeExecution {

    /** The Maven property specifying the version. */
    public abstract String getProperty();

    /**
     * The minimum version needed. The version will be dynamically updated to this version (but only
     * if necessary).
     */
    public abstract String getMinimumVersion();

    /**
     * The minimum plugin parent POM version needed. If the plugin under test is on an older plugin
     * parent POM, the property will not be updated. Return {@code null} to skip the minimum plugin
     * POM version check.
     */
    public String getMinimumPluginParentPomVersion() {
        return null;
    }

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        PluginCompatTesterConfig config = context.getConfig();
        MavenRunner runner =
                new ExternalMavenRunner(
                        config.getExternalMaven(),
                        config.getMavenSettings(),
                        config.getMavenArgs());
        File pluginDir = context.getPluginDir();
        if (pluginDir != null) {
            try {
                if (getMinimumPluginParentPomVersion() != null) {
                    String pluginParentPomVersion = getPluginParentPomVersion(pluginDir, runner);
                    if (new VersionNumber(pluginParentPomVersion)
                            .isOlderThan(new VersionNumber(getMinimumPluginParentPomVersion()))) {
                        return false;
                    }
                }
                String version = getPropertyVersion(pluginDir, getProperty(), runner);
                return new VersionNumber(version)
                        .isOlderThan(new VersionNumber(getMinimumVersion()));
            } catch (PomExecutionException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        context.getArgs().add(String.format("-D%s=%s", getProperty(), getMinimumVersion()));
    }

    private static String getPluginParentPomVersion(File pluginPath, MavenRunner runner)
            throws PomExecutionException {
        String cur;
        for (cur = "project.parent"; ; cur += ".parent") {
            String groupId = getPropertyVersion(pluginPath, cur + ".groupId", runner);
            String artifactId = getPropertyVersion(pluginPath, cur + ".artifactId", runner);
            if (groupId == null || artifactId == null) {
                return null;
            }
            if (groupId.equals("org.jenkins-ci.plugins") && artifactId.equals("plugin")) {
                return getPropertyVersion(pluginPath, cur + ".version", runner);
            }
        }
    }

    private static String getPropertyVersion(File pluginPath, String property, MavenRunner runner)
            throws PomExecutionException {
        Path log = pluginPath.toPath().resolve(property + ".log");
        runner.run(
                Map.of("expression", property, "output", log.toAbsolutePath().toString()),
                pluginPath,
                null,
                "-q",
                "help:evaluate");
        String output;
        try {
            output = Files.readString(log, Charset.defaultCharset()).trim();
            Files.deleteIfExists(log);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "null object or invalid expression".equals(output) ? null : output;
    }
}
