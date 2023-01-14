package org.jenkins.tools.test.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.util.ExecutedTestNamesSolver;

/**
 * Runs external Maven executable.
 */
public class ExternalMavenRunner implements MavenRunner {

    private static final Logger LOGGER = Logger.getLogger(ExternalMavenRunner.class.getName());

    private static final String DISABLE_DOWNLOAD_LOGS = "-ntp";

    @CheckForNull
    private File mvn;

    private Set<String> executedTests;

    /**
     * Constructor.
     * 
     * @param mvn Path to Maven. If {@code null}, a default Maven executable from
     *            {@code PATH} will be used
     */
    public ExternalMavenRunner(@CheckForNull File mvn) {
        this.mvn = mvn;
        this.executedTests = new HashSet<>();
    }

    public Set<String> getExecutedTests() {
        return Collections.unmodifiableSet(executedTests);
    }

    @Override
    public void run(Config config, File baseDirectory, File buildLogFile, String... goals)
            throws PomExecutionException {
        List<String> cmd = new ArrayList<>();
        cmd.add(mvn != null ? mvn.getAbsolutePath() : "mvn");
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
        cmd.addAll(Arrays.asList(goals));
        LOGGER.log(Level.INFO, "Running {0} in {1} >> {2}" + buildLogFile, new Object[]{String.join(" ", cmd), baseDirectory, buildLogFile});
        try {
            Process p = new ProcessBuilder(cmd).directory(baseDirectory).redirectErrorStream(true).start();
            List<String> succeededPluginArtifactIds = new ArrayList<>();
            try (InputStream is = p.getInputStream();
                    BufferedReader r = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
                    FileOutputStream os = new FileOutputStream(buildLogFile, true);
                    PrintWriter w = new PrintWriter(new OutputStreamWriter(os, Charset.defaultCharset()))) {
                String completed = null;
                Pattern pattern = Pattern.compile("\\[INFO\\] --- (.+):.+:.+ [(].+[)] @ .+ ---");
                String line;
                boolean testPhase = false;
                while ((line = r.readLine()) != null) {
                    System.out.println(line);
                    w.println(line);
                    if(line.contains("T E S T S")) {
                        testPhase = true;
                    }
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        if (completed != null) {
                            succeededPluginArtifactIds.add(completed);
                        }
                        completed = m.group(1);
                    } else if (line.equals("[INFO] BUILD SUCCESS") && completed != null) {
                        succeededPluginArtifactIds.add(completed);
                    } else if (testPhase && line.startsWith("[INFO] Running") && !line.contains("InjectedTest")) {
                        this.executedTests.add(line.split("Running")[1].trim());
                    }
                }
                w.flush();
                LOGGER.log(Level.INFO, "Succeeded artifact IDs: {0}", String.join(",", succeededPluginArtifactIds));
                LOGGER.log(Level.INFO, "Executed tests: {0}", String.join(",", getExecutedTests()));
            }
            if (p.waitFor() != 0) {
                throw new PomExecutionException(cmd + " failed in " + baseDirectory, succeededPluginArtifactIds,
                        /* TODO */Collections.emptyList(), Collections.emptyList(),
                        new ExecutedTestNamesSolver().solve(getTypes(config), getExecutedTests(), baseDirectory));
            }
        } catch (PomExecutionException x) {
            LOGGER.log(Level.WARNING, "Failed to run Maven", x);
            throw x;
        } catch (Exception x) {
            LOGGER.log(Level.WARNING, "Failed to run Maven", x);
            throw new PomExecutionException(x);
        }
    }

    private Set<String> getTypes(Config config) {
        Set<String> result = new HashSet<>();
        if (config == null || config.userProperties == null || !config.userProperties.containsKey("types")) {
            result.add("surefire");
            return result;
        }
        String types = config.userProperties.get("types");
        for (String type : types.split(",")) {
            result.add(type);
        }
        return result;
    }

}
