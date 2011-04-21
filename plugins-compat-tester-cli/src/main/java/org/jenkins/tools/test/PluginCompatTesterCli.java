package org.jenkins.tools.test;

import org.codehaus.plexus.PlexusContainerException;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PluginCompatTesterCli {

    public static void main(String[] args) throws IOException, PlexusContainerException {
        String updateCenterUrl = System.getProperty("updateCenterUrl");
        String parentCoord = System.getProperty("parentCoordinates");
        String workDirectory = System.getProperty("workDirectory");
        String reportFile = System.getProperty("reportFile");
        String pluginsListParam = System.getProperty("pluginsList");

        if(workDirectory == null){
            throw new IllegalArgumentException("Parameter -DworkDirectory should be passed to the CLI !");
        }
        if(!new File(workDirectory).exists()){
            throw new IllegalArgumentException("Parameter -DworkDirectory should be referencing an existing directory !\");");
        }
        if(reportFile == null){
            throw new IllegalArgumentException("Parameter -DreportFile should be passed to the CLI !");
        }

        PluginCompatTesterConfig config = null;
        if(updateCenterUrl != null || (parentCoord != null && !"".equals(parentCoord))){
            config = new PluginCompatTesterConfig(updateCenterUrl, parentCoord, new File(workDirectory), new File(reportFile));
        } else {
            config = new PluginCompatTesterConfig(new File(workDirectory), new File(reportFile));
        }

        if(pluginsListParam != null && !"".equals(pluginsListParam)){
            List<String> pluginsList = Arrays.asList(pluginsListParam.split(","));
            config.setPluginsList(pluginsList);
        }

        PluginCompatTester tester = new PluginCompatTester(config);
        tester.testPlugins();
    }
}
