package org.jenkins.tools.test.maven;

import java.io.File;
import java.util.Map;
import org.jenkins.tools.test.exception.PomExecutionException;

public interface MavenRunner {

    void run(Map<String, String> properties, File baseDirectory, File buildLogFile, String... args)
            throws PomExecutionException;
}
