package org.jenkins.tools.test.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
public class BuildSystemUtils {
    public static BuildSystem detectBuildSystem(File cloneDir) {
        for (String gradleBuildFile : BuildSystem.GRADLE.getBuildFiles()) {
            File buildFile = new File(cloneDir, gradleBuildFile);
            if (buildFile.exists()) {
                return BuildSystem.GRADLE;
            }
        }

        return BuildSystem.MAVEN;
    }
}
