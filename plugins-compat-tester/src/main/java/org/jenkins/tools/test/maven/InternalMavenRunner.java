package org.jenkins.tools.test.maven;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.logging.Logger;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.logging.SystemIOLoggerFilter;

public class InternalMavenRunner implements MavenRunner {

    private MavenEmbedder embedder;

    private void init(Config config) throws MavenEmbedderException {
        if (embedder != null) {
            return;
        }
        //here we don't care about paths for build the embedder
        MavenRequest mavenRequest = buildMavenRequest(config, null, null);
        embedder = new MavenEmbedder(Thread.currentThread().getContextClassLoader(), mavenRequest);
    }

    @Override public void run(Config config, File baseDirectory, File buildLogFile, String... goals) throws PomExecutionException {
        try {
            init(config);
        } catch (Exception x) {
            throw new PomExecutionException(x);
        }
        final List<String> succeededPlugins = new ArrayList<>();
        try {
            MavenRequest mavenRequest = buildMavenRequest(config, baseDirectory.getAbsolutePath(),
                    config.userSettingsFile == null
                    ? null
                    : config.userSettingsFile.getAbsolutePath());
            mavenRequest.setGoals(Arrays.asList(goals));
            mavenRequest.setPom(new File(baseDirectory, "pom.xml").getAbsolutePath());
            AbstractExecutionListener mavenListener = new AbstractExecutionListener() {
                @Override public void mojoSucceeded(ExecutionEvent event) {
                    succeededPlugins.add(event.getMojoExecution().getArtifactId());
                }
            };
            mavenRequest.setExecutionListener(mavenListener);

            mavenRequest.setLoggingLevel(Logger.LEVEL_INFO);

            final PrintStream originalOut = System.out;
            final PrintStream originalErr = System.err;
            SystemIOLoggerFilter loggerFilter = new SystemIOLoggerFilter(buildLogFile);

            // Since here, we are replacing System.out & System.err by
            // wrappers logging things in the build log file
            // We can't do this by using maven embedder's logger (or plexus logger)
            // since :
            // - It would imply to Instantiate a new MavenEmbedder for every test (which have a performance/memory cost !)
            // - Plus it looks like there are lots of System.out/err.println() in maven
            // plugin (instead of using maven logger)
            System.setOut(new SystemIOLoggerFilter.SystemIOWrapper(loggerFilter, originalOut));
            System.setErr(new SystemIOLoggerFilter.SystemIOWrapper(loggerFilter, originalErr));

            try {
                executeGoals(embedder, mavenRequest);
            } catch (PomExecutionException x) {
                PomExecutionException x2 = new PomExecutionException(x);
                x2.succeededPluginArtifactIds.addAll(succeededPlugins);
                throw x2;
            } finally {
                // Setting back System.out/err
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        } catch (IOException x) {
            throw new PomExecutionException(x);
        }
    }

    private static MavenRequest buildMavenRequest(Config config, String rootDir,String settingsPath) {

        MavenRequest mavenRequest = new MavenRequest();

        mavenRequest.setBaseDirectory(rootDir);

        mavenRequest.setUserSettingsFile(settingsPath);

        mavenRequest.getUserProperties().putAll(config.userProperties);

        return mavenRequest;

    }

    private static void executeGoals(MavenEmbedder mavenEmbedder, MavenRequest mavenRequest) throws PomExecutionException {

        MavenExecutionResult result;
        try {
            result = mavenEmbedder.execute(mavenRequest);
        }catch(MavenEmbedderException e){
            // TODO: better manage this exception
            throw new RuntimeException("Error during maven embedder execution", e);
        }

        if(!result.getExceptions().isEmpty()){
            // If at least one OOME is thrown, rethrow it "as is"
            // It must be treated a an internal error instead of a PomExecutionException !
            for(Throwable t : result.getExceptions()){
                if(t instanceof OutOfMemoryError){
                    throw (OutOfMemoryError)t;
                }
            }
            throw new PomExecutionException("Error while executing pom goals : "+ mavenRequest.getGoals(), Collections.emptyList(), result.getExceptions(), Collections.emptyList());
        }
    }

}
