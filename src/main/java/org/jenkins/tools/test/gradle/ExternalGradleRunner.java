package org.jenkins.tools.test.gradle;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.SystemUtils;
import org.jenkins.tools.test.exception.GradleExecutionException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

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

        File gradleWrapper = new File(baseDirectory, SystemUtils.IS_OS_WINDOWS ? "gradlew.bat" : "gradlew");
        if (externalGradle != null) {
            cmd.add(externalGradle.getAbsolutePath());
        } else if (gradleWrapper.exists() && gradleWrapper.canExecute()) {
            cmd.add(gradleWrapper.getAbsolutePath());
        } else {
            cmd.add("gradle");
        }

        cmd.add("--console=plain");
        cmd.add("--stacktrace");
        cmd.add("--info");

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

    private static class GradleGobbler extends Thread {

        @NonNull
        private final Process p;

        @CheckForNull
        private final File buildLogFile;

        public GradleGobbler(@NonNull Process p, @Nullable File buildLogFile) {
            this.p = p;
            this.buildLogFile = buildLogFile;
        }

        @Override
        public void run() {
            try (InputStream is = p.getInputStream();
                    Reader isr = new InputStreamReader(is, Charset.defaultCharset());
                    BufferedReader r = new BufferedReader(isr);
                    OutputStream os = buildLogFile == null
                            ? OutputStream.nullOutputStream()
                            : new FileOutputStream(buildLogFile, true);
                    Writer osw = new OutputStreamWriter(os, Charset.defaultCharset());
                    PrintWriter w = new PrintWriter(osw)) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println(line);
                    w.println(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
