package org.jenkins.tools.test.gradle;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.SystemUtils;
import org.jenkins.tools.test.exception.GradleExecutionException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.util.ProcessOutputGobbler;

public class ExternalGradleRunner implements GradleRunner {

    private static final Logger LOGGER = Logger.getLogger(ExternalGradleRunner.class.getName());

    @CheckForNull
    private final File externalGradle;

    @CheckForNull
    private final File gradleProperties;

    @NonNull
    private final List<String> gradleArgs;

    public ExternalGradleRunner(
            @CheckForNull File externalGradle, @CheckForNull File gradleProperties, @Nullable List<String> gradleArgs) {
        this.externalGradle = externalGradle;
        this.gradleProperties = gradleProperties;
        this.gradleArgs = gradleArgs == null ? List.of() : List.copyOf(gradleArgs);
    }

    public ExternalGradleRunner(@NonNull PluginCompatTesterConfig config) {
        this(config.getExternalGradle(), config.getGradleProperties(), config.getGradleArgs());
    }

    @Override
    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "intended behavior")
    public void run(
            Map<String, String> properties,
            File baseDirectory,
            @CheckForNull String moduleName,
            @CheckForNull File buildLogFile,
            String... tasks)
            throws GradleExecutionException {

        List<String> cmd = new ArrayList<>();

        if (externalGradle != null) {
            cmd.add(externalGradle.getAbsolutePath());
        } else {
            cmd.add(SystemUtils.IS_OS_WINDOWS ? "gradle.bat" : "gradle");
        }

        if (gradleProperties != null && gradleProperties.exists()) {
            cmd.add("-P");
            cmd.add("gradle.properties=" + gradleProperties.getAbsolutePath());
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            cmd.add("-P" + entry.getKey() + "=" + entry.getValue());
        }

        cmd.addAll(gradleArgs);

        cmd.addAll(List.of(tasks));

        if (buildLogFile != null) {
            LOGGER.log(Level.INFO, "Running {0} in {1} >> {2}", new Object[] {
                String.join(" ", cmd), baseDirectory, buildLogFile
            });
        } else {
            LOGGER.log(Level.INFO, "Running {0} in {1}", new Object[] {String.join(" ", cmd), baseDirectory});
        }

        Process p;
        try {
            p = new ProcessBuilder(cmd)
                    .directory(baseDirectory)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        GradleGobbler gobbler = new GradleGobbler(p, buildLogFile);
        gobbler.start();

        int exitStatus;
        try {
            exitStatus = p.waitFor();
            gobbler.join();
        } catch (InterruptedException e) {
            throw new GradleExecutionException(String.join(" ", cmd) + " was interrupted", e);
        }
        if (exitStatus != 0) {
            throw new GradleExecutionException(
                    String.join(" ", cmd) + " in " + baseDirectory + " failed with exit status " + exitStatus);
        }
    }

    private static class GradleGobbler extends ProcessOutputGobbler {
        public GradleGobbler(@NonNull Process p, @CheckForNull File buildLogFile) {
            super(p, buildLogFile);
        }
    }
}
