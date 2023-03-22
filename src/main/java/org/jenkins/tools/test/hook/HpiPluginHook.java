package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * The {@code overrideWar} option is available in HPI Plugin 3.29 or later, but many plugins under
 * test still use an older plugin parent POM and therefore an older HPI plugin version. As a
 * temporary workaround, we override the HPI plugin version to a recent version. When all plugins in
 * the managed set are using a plugin parent POM with HPI Plugin 3.29 or later (i.e., plugin parent
 * POM 4.44 or later), this can be deleted.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class HpiPluginHook extends PluginCompatTesterHookBeforeExecution {

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
                String version = getHpiPluginVersion(pluginDir, runner);
                // TODO pending release of "multimodule" branch in "maven-hpi-plugin"
                return new VersionNumber(version).isOlderThan(new VersionNumber("3.40-rc1394.915b_0b_17732d"));
            } catch (PomExecutionException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        // TODO pending release of "multimodule" branch in "maven-hpi-plugin"
        context.getArgs().add("-Dhpi-plugin.version=3.40-rc1394.915b_0b_17732d");
    }

    private static String getHpiPluginVersion(File pluginPath, MavenRunner runner)
            throws PomExecutionException {
        File log = new File(pluginPath, "hpi-plugin.log");
        runner.run(
                Map.of("expression", "hpi-plugin.version", "output", log.getAbsolutePath()),
                pluginPath,
                null,
                "-q",
                "help:evaluate");
        List<String> output;
        try {
            output = Files.readAllLines(log.toPath(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.get(output.size() - 1);
    }
}
