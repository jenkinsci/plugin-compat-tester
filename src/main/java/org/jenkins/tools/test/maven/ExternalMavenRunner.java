package org.jenkins.tools.test.maven;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
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
import org.jenkins.tools.test.exception.PomExecutionException;

/** Runs external Maven executable. */
public class ExternalMavenRunner implements MavenRunner {

    private static final Logger LOGGER = Logger.getLogger(ExternalMavenRunner.class.getName());

    @CheckForNull private final File externalMaven;

    @CheckForNull private final File m2Settings;

    @NonNull private final List<String> mavenArgs;

    /**
     * Constructor.
     *
     * @param externalMaven Path to Maven. If {@code null}, a default Maven executable from {@code
     *     PATH} will be used
     */
    public ExternalMavenRunner(
            @CheckForNull File externalMaven,
            @CheckForNull File m2Settings,
            @NonNull List<String> mavenArgs) {
        this.externalMaven = externalMaven;
        this.m2Settings = m2Settings;
        this.mavenArgs = mavenArgs;
    }

    @Override
    public void run(
            Map<String, String> properties, File baseDirectory, File buildLogFile, String... args)
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
        if (m2Settings != null) {
            cmd.add("-s");
            cmd.add(m2Settings.toString());
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            cmd.add("-D" + entry);
        }
        cmd.addAll(mavenArgs);
        cmd.addAll(List.of(args));
        LOGGER.log(
                Level.INFO,
                "Running {0} in {1} >> {2}",
                new Object[] {String.join(" ", cmd), baseDirectory, buildLogFile});
        Process p;
        try {
            p = new ProcessBuilder(cmd).directory(baseDirectory).redirectErrorStream(true).start();
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
            throw new PomExecutionException(cmd + " was interrupted", e);
        }
        if (exitStatus != 0) {
            throw new PomExecutionException(
                    cmd + " in " + baseDirectory + " failed with exit status " + exitStatus);
        }
    }

    private static class MavenGobbler extends Thread {

        @NonNull private final Process p;

        @NonNull private final File buildLogFile;

        public MavenGobbler(@NonNull Process p, @NonNull File buildLogFile) {
            this.p = p;
            this.buildLogFile = buildLogFile;
        }

        @Override
        public void run() {
            try (InputStream is = p.getInputStream();
                    Reader isr = new InputStreamReader(is, Charset.defaultCharset());
                    BufferedReader r = new BufferedReader(isr);
                    OutputStream os = new FileOutputStream(buildLogFile, true);
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
