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

    private static final String DISABLE_DOWNLOAD_LOGS = "-ntp";

    @CheckForNull private File mvn;

    /**
     * Constructor.
     *
     * @param mvn Path to Maven. If {@code null}, a default Maven executable from {@code PATH} will
     *     be used
     */
    public ExternalMavenRunner(@CheckForNull File mvn) {
        this.mvn = mvn;
    }

    @Override
    public void run(Config config, File baseDirectory, File buildLogFile, String... goals)
            throws PomExecutionException {
        List<String> cmd = new ArrayList<>();
        if (mvn != null) {
            cmd.add(mvn.getAbsolutePath());
        } else {
            cmd.add(SystemUtils.IS_OS_WINDOWS ? "mvn.cmd" : "mvn");
        }
        cmd.add("--show-version");
        cmd.add("--batch-mode");
        cmd.add("--errors");
        cmd.add(DISABLE_DOWNLOAD_LOGS);
        if (config.userSettingsFile != null) {
            cmd.add("--settings=" + config.userSettingsFile);
        }
        for (Map.Entry<String, String> entry : config.userProperties.entrySet()) {
            cmd.add("--define=" + entry);
        }
        cmd.addAll(config.mavenOptions);
        cmd.addAll(List.of(goals));
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
