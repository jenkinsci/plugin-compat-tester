package org.jenkins.tools.test.gradle;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.util.Map;
import org.jenkins.tools.test.exception.GradleExecutionException;

public interface GradleRunner {
    void run(
            Map<String, String> properties,
            File baseDirectory,
            @CheckForNull String moduleName,
            @CheckForNull File buildLogFile,
            String... tasks)
            throws GradleExecutionException;
}
