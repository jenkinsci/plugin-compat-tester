package org.jenkins.tools.test.hook;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.InternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BOAndDPCompileHook extends PluginCompatTesterHookBeforeCompile {

    protected MavenRunner runner;
    protected MavenRunner.Config mavenConfig;
    
    public BOAndDPCompileHook() {
        System.out.println("Loaded TransformPomToEffectiveOne");
    }

    @Override
    public List<String> transformedPlugins() {
        List<String> transformedPlugins = new LinkedList<>();
        transformedPlugins.addAll(BlueOceanHook.BO_PLUGINS);
        transformedPlugins.addAll(DeclarativePipelineHook.DP_PLUGINS);
        return transformedPlugins;
    }

    @Override
    public void validate(Map<String, Object> toCheck) throws Exception {
        //Empty by design
    }

    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        try {
            System.out.println("Executing BO and DO compile hook");
            PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
            MavenCoordinates core = (MavenCoordinates) moreInfo.get("core");

            runner = config.getExternalMaven() == null ? new InternalMavenRunner() : new ExternalMavenRunner(config.getExternalMaven());
            mavenConfig = getMavenConfig(config);

            File pluginDir = (File) moreInfo.get("pluginDir");

            // We need to compile before generating effective pom overriding jenkins.version
            // only if the plugin is not already compiled
            boolean ranCompile = moreInfo.containsKey(OVERRIDE_DEFAULT_COMPILE) ? (boolean) moreInfo.get(OVERRIDE_DEFAULT_COMPILE) : false;
            if (!ranCompile) {
                compile(mavenConfig, pluginDir);
                moreInfo.put(OVERRIDE_DEFAULT_COMPILE, true);
            }

            // Now we can generate effective pom
            generateEffectivePom(mavenConfig, pluginDir, core);

            System.out.println("Executed BO and DP compile hook");
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

    private void generateEffectivePom(MavenRunner.Config mavenConfig, File path, MavenCoordinates core) throws PomExecutionException {
        System.out.println("Generating effective pom in " + path);
        File effectivePomLogfile = new File(path + "/effectivePomTransformationLog.log");
        runner.run(mavenConfig, path, effectivePomLogfile, "help:effective-pom", "-Doutput=pom.xml", "-Djenkins.version=" + core.version);
    }

    private void compile(MavenRunner.Config mavenConfig, File path) throws PomExecutionException, IOException {
        System.out.println("Cleaning up node modules if neccessary");
        removeNodeFolders(path);
        System.out.println("Compile plugin log in " + path);
        File compilePomLogfile = new File(path + "/compilePluginLog.log");
        runner.run(mavenConfig, path, compilePomLogfile, "clean", "process-test-classes", "-Dmaven.javadoc.skip");
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