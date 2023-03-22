package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeCompilationContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeCompile.class)
public class VersionValidationHook extends PluginCompatTesterHookBeforeCompile {
    @Override
    public void action(@NonNull BeforeCompilationContext context)
            throws PluginCompatibilityTesterException {
        PluginCompatTesterConfig config = context.getConfig();
        MavenRunner runner =
                new ExternalMavenRunner(
                        config.getExternalMaven(),
                        config.getMavenSettings(),
                        config.getMavenArgs());
        File pluginDir = context.getPluginDir();
        if (pluginDir != null) {
            String version = getVersion(pluginDir, runner);
            if (version.endsWith("-SNAPSHOT")) {
                throw new PluginSourcesUnavailableException(
                        "Failed to check out plugin sources for " + version);
            }
        }
    }

    private static String getVersion(File pluginPath, MavenRunner runner)
            throws PomExecutionException {
        String property = "project.version";
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
        return output;
    }
}
