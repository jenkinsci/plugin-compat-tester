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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.util.ExecutedTestNamesSolver;

/**
 * Runs external Maven executable.
 */
public class ExternalMavenRunner implements MavenRunner {

    private static final String DISABLE_DOWNLOAD_LOGS = "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn";

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
        cmd.add(DISABLE_DOWNLOAD_LOGS);
        if (config.userSettingsFile != null) {
            cmd.add("--settings=" + config.userSettingsFile);
        }
        for (Map.Entry<String, String> entry : config.userProperties.entrySet()) {
            cmd.add("--define=" + entry);
        }
        cmd.addAll(Arrays.asList(goals));
        System.out.println("running " + cmd + " in " + baseDirectory + " >> " + buildLogFile);
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
                System.out.println("succeeded artifactIds: " + succeededPluginArtifactIds);
                System.out.println("executed classname tests: " + getExecutedTests());
            }
            if (p.waitFor() != 0) {
                throw new PomExecutionException(cmd + " failed in " + baseDirectory, succeededPluginArtifactIds,
                        /* TODO */Collections.emptyList(), Collections.emptyList(),
                        new ExecutedTestNamesSolver().solve(getExecutedTests(), baseDirectory));
            }
        } catch (PomExecutionException x) {
            x.printStackTrace();
            throw x;
        } catch (Exception x) {
            x.printStackTrace();
            throw new PomExecutionException(x);
        }
    }

}