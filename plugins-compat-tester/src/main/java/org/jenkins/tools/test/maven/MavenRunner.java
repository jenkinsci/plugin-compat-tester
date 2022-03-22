package org.jenkins.tools.test.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jenkins.tools.test.exception.PomExecutionException;

public interface MavenRunner {

    void run(Config config, File baseDirectory, File buildLogFile, String... goals) throws PomExecutionException;

    class Config {
        public File userSettingsFile;
        public final Map<String,String> userProperties = new TreeMap<>();
        public List<String> mavenProfiles = new ArrayList<>();
    }

}
