package org.jenkins.tools.test.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkins.tools.test.exception.PomExecutionException;

import javax.annotation.CheckForNull;

/**
 * Runs external Maven executable.
 */
public class ExternalMavenRunner implements MavenRunner {

    @CheckForNull
    private File mvn;

    /**
     * Constructor.
     * @param mvn Path to Maven.
     *            If {@code null}, a default Maven executable from {@code PATH} will be used
     */
    public ExternalMavenRunner(@CheckForNull File mvn) {
        this.mvn = mvn;
    }

    @Override
    public void run(Config config, File baseDirectory, File buildLogFile, String... goals) throws PomExecutionException {
        List<String> cmd = new ArrayList<>();
        cmd.add(mvn != null ? mvn.getAbsolutePath() : "mvn");
        cmd.add("--show-version");
        cmd.add("--batch-mode");
        if (config.userSettingsFile != null) {
            cmd.add("--settings=" + config.userSettingsFile);
        }
        for (Map.Entry<String,String> entry : config.userProperties.entrySet()) {
            cmd.add("--define=" + entry);
        }
        cmd.addAll(Arrays.asList(goals));
        System.out.println("running " + cmd + " in " + baseDirectory + " >> " + buildLogFile);
        try {
            Process p = new ProcessBuilder(cmd).directory(baseDirectory).redirectErrorStream(true).start();
            List<String> succeededPluginArtifactIds = new ArrayList<>();
            try (InputStream is = p.getInputStream(); FileOutputStream os = new FileOutputStream(buildLogFile, true); PrintWriter w = new PrintWriter(os)) {
                String completed = null;
                Pattern pattern = Pattern.compile("\\[INFO\\] --- (.+):.+:.+ [(].+[)] @ .+ ---");
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println(line);
                    w.println(line);
                    Matcher m = pattern.matcher(line);
                    if (m.matches()) {
                        if (completed != null) {
                            succeededPluginArtifactIds.add(completed);
                        }
                        completed = m.group(1);
                    } else if (line.equals("[INFO] BUILD SUCCESS") && completed != null) {
                        succeededPluginArtifactIds.add(completed);
                    }
                }
                w.flush();
                System.out.println("succeeded artifactIds: " + succeededPluginArtifactIds);
            }
            if (p.waitFor() != 0) {
                throw new PomExecutionException(cmd + " failed in " + baseDirectory, succeededPluginArtifactIds, /* TODO */Collections.emptyList(), Collections.emptyList());
            }
        } catch (PomExecutionException x) {
            throw x;
        } catch (Exception x) {
            throw new PomExecutionException(x);
        }
    }

}
