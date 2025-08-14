package org.jenkins.tools.test.util;

public enum BuildSystem {
    MAVEN("pom.xml"),
    GRADLE("build.gradle", "build.gradle.kts");

    private final String[] buildFiles;

    BuildSystem(String... buildFiles) {
        this.buildFiles = buildFiles;
    }

    public String[] getBuildFiles() {
        return buildFiles;
    }
}
