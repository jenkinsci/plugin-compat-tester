package org.jenkins.tools.test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.codehaus.plexus.PlexusContainerException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

import java.io.IOException;
import java.util.Arrays;

public class PluginCompatTesterCli {

    public static void main(String[] args) throws IOException, PlexusContainerException {
        CliOptions options = new CliOptions();
        JCommander jcommander = null;
        try {
            jcommander = new JCommander(options, args);
        }catch(ParameterException e){
            System.err.println(e.getMessage());
            if(jcommander == null){
                jcommander = new JCommander(options);
            }
            jcommander.usage();
            System.exit(1);
        }

        options.getWorkDirectory().mkdirs();

        PluginCompatTesterConfig config = new PluginCompatTesterConfig(options.getUpdateCenterUrl(), options.getParentCoord(),
                options.getWorkDirectory(), options.getReportFile(), options.getM2SettingsFile());

        if(options.getIncludePlugins() != null && !options.getIncludePlugins().isEmpty()){
            config.setIncludePlugins(Arrays.asList(options.getIncludePlugins().toLowerCase().split(",")));
        }
        if(options.getSkipTestCache() != null){
            config.setSkipTestCache(Boolean.valueOf(options.getSkipTestCache()).booleanValue());
        }
        if(options.getTestCacheTimeout() != null){
            config.setTestCacheTimeout(options.getTestCacheTimeout().longValue());
        }

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }
}
