package org.jenkins.tools.test.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

public interface MavenRunner {

    void run(Config config, File baseDirectory, File buildLogFile, String... goals)
            throws PomExecutionException;

    class Config {

        public final File userSettingsFile;

        public final Map<String, String> userProperties = new TreeMap<>();

        public final List<String> mavenOptions;

        public Config(PluginCompatTesterConfig pctConfig) throws IOException {
            userSettingsFile = pctConfig.getM2SettingsFile();
            userProperties.putAll(pctConfig.retrieveMavenProperties());
            mavenOptions = pctConfig.getMavenOptions();
        }
    }
}
