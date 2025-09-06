package org.jenkins.tools.test.maven;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
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
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.util.ProcessOutputGobbler;

/** Runs external Maven executable. */
public class ExternalMavenRunner implements MavenRunner {

    private static final Logger LOGGER = Logger.getLogger(ExternalMavenRunner.class.getName());

    @CheckForNull
    private final File externalMaven;

    @CheckForNull
    private final File mavenSettings;

    @NonNull
    private final List<String> mavenArgs;

    /**
     * Constructor.
     *
     * @param externalMaven Path to Maven. If {@code null}, a default Maven executable from {@code
     *     PATH} will be used
     */
    public ExternalMavenRunner(
            @CheckForNull File externalMaven, @CheckForNull File mavenSettings, @NonNull List<String> mavenArgs) {
        this.externalMaven = externalMaven;
        this.mavenSettings = mavenSettings;
        this.mavenArgs = mavenArgs;
    }

    /**
     * @param config The PCT configuration to extract maven arguments from.
     */
    public ExternalMavenRunner(@NonNull PluginCompatTesterConfig config) {
        this(config.getExternalMaven(), config.getMavenSettings(), config.getMavenArgs());
    }

    @Override
    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "intended behavior")
    public void run(
            Map<String, String> properties, File baseDirectory, String moduleName, File buildLogFile, String... args)
            throws PomExecutionException {
        List<String> cmd = new ArrayList<>();
        if (externalMaven != null) {
            cmd.add(externalMaven.getAbsolutePath());
        } else {
            cmd.add(SystemUtils.IS_OS_WINDOWS ? "mvn.cmd" : "mvn");
        }
        cmd.add("-B"); // --batch-mode
        cmd.add("-V"); // --show-version
        cmd.add("-e"); // --errors
        cmd.add("-ntp"); // --no-transfer-progress
        if (mavenSettings != null) {
            cmd.add("-s");
            cmd.add(mavenSettings.toString());
        }
        if (moduleName != null && !moduleName.isBlank()) {
            cmd.add("-pl");
            cmd.add(moduleName);
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            cmd.add("-D" + entry);
        }
        cmd.addAll(mavenArgs);
        cmd.addAll(List.of(args));
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
        MavenGobbler gobbler = new MavenGobbler(p, buildLogFile);
        gobbler.start();
        int exitStatus;
        try {
            exitStatus = p.waitFor();
            gobbler.join();
        } catch (InterruptedException e) {
            throw new PomExecutionException(String.join(" ", cmd) + " was interrupted", e);
        }
        if (exitStatus != 0) {
            throw new PomExecutionException(
                    String.join(" ", cmd) + " in " + baseDirectory + " failed with exit status " + exitStatus);
        }
    }

    private static class MavenGobbler extends ProcessOutputGobbler {
        public MavenGobbler(@NonNull Process p, @CheckForNull File buildLogFile) {
            super(p, buildLogFile);
        }
    }
}
