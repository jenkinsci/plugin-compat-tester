package org.jenkins.tools.test.hook;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.InternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This hook removes the node and node_modules folders from BO before compiling it
 */
public class RemoveNodeHook extends PluginCompatTesterHookBeforeCompile {

    protected MavenRunner runner;
    protected MavenRunner.Config mavenConfig;

    public RemoveNodeHook() {
        System.out.println("Loaded RemoveNodeHook");
    }

    @Override
    public List<String> transformedPlugins() {
        List<String> transformedPlugins = new LinkedList<>();
        transformedPlugins.addAll(BlueOceanHook.BO_PLUGINS);
        return transformedPlugins;
    }

    @Override
    public void validate(Map<String, Object> toCheck) throws Exception {
        //Empty by design
    }

    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        try {
            System.out.println("Executing RemoveNodeHook hook");
            PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");

            runner = config.getExternalMaven() == null ? new InternalMavenRunner() : new ExternalMavenRunner(config.getExternalMaven());
            mavenConfig = getMavenConfig(config);

            File pluginDir = (File) moreInfo.get("pluginDir");
            removeNodeFolders(pluginDir);
            System.out.println("Executed RemoveNodeHook hook");
            return moreInfo;
            // Exceptions get swallowed, so we print to console here and rethrow again
        } catch (Exception e) {
            System.out.println("Exception executing hook");
            System.out.println(e);
            throw e;
        }
    }

    private MavenRunner.Config getMavenConfig(PluginCompatTesterConfig config) throws IOException {
        MavenRunner.Config mconfig = new MavenRunner.Config();
        mconfig.userSettingsFile = config.getM2SettingsFile();
        // TODO REMOVE
        mconfig.userProperties.put( "failIfNoTests", "false" );
        mconfig.userProperties.put( "argLine", "-XX:MaxPermSize=128m" );
        String mavenPropertiesFilePath = config.getMavenPropertiesFile();
        if ( StringUtils.isNotBlank( mavenPropertiesFilePath )) {
            File file = new File (mavenPropertiesFilePath);
            if (file.exists()) {
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream( file );
                    Properties properties = new Properties(  );
                    properties.load( fileInputStream  );
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        mconfig.userProperties.put((String) entry.getKey(), (String) entry.getValue());
                    }
                } finally {
                    IOUtils.closeQuietly( fileInputStream );
                }
            } else {
                System.out.println("File " + mavenPropertiesFilePath + " not exists" );
            }
        }
        return mconfig;
    }

    private void removeNodeFolders(File path) throws PomExecutionException, IOException {
        File nodeFolder = new File(path, "node");
        if (nodeFolder.exists() && nodeFolder.isDirectory()) {
           FileUtils.deleteDirectory(nodeFolder);
        }
        File nodeModulesFolder = new File(path, "node_modules");
        if (nodeModulesFolder.exists() && nodeModulesFolder.isDirectory()) {
            FileUtils.deleteDirectory(nodeModulesFolder);
        }
    }
}