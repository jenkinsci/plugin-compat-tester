package org.jenkins.tools.test.hook;

import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NodeCleanupBeforeCompileHook extends PluginCompatTesterHookBeforeCompile {

    protected MavenRunner runner;
    protected MavenRunner.Config mavenConfig;

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
        boolean shouldExecuteHook = config.getIncludePlugins().contains("sse-gateway");
        runner = new ExternalMavenRunner(config.getExternalMaven());
        mavenConfig = getMavenConfig(config);

        if (shouldExecuteHook) {
            File pluginDir = (File) moreInfo.get("pluginDir");
            System.out.println("--> Plugin dir is " + pluginDir);
            System.out.println("--> This is sse-gateway");
            // TODO probably do not need
            // boolean ranCompile = moreInfo.containsKey(OVERRIDE_DEFAULT_COMPILE) && (boolean) moreInfo.get(OVERRIDE_DEFAULT_COMPILE);
            // if (!ranCompile) {
            //     compile(mavenConfig, pluginDir);
            //     moreInfo.put(OVERRIDE_DEFAULT_COMPILE, true);
            // }
            try {
                System.out.println("--> Executing node and node_modules cleanup hook");
                System.out.println("--> Compiling");
                compile(mavenConfig, pluginDir);
                return moreInfo;
            } catch (Exception e) {
                System.out.println("Exception executing hook");
                System.out.println(e);
                throw e;
            }
        } else {
            System.out.println("--> Not SSE Gateway so skipping hook");
            return moreInfo;
        }
    }

    @Override
    public void validate(Map<String, Object> toCheck) {
    }

    private void compile(MavenRunner.Config mavenConfig, File path) throws PomExecutionException, IOException {
        System.out.println("--> Calling removeNodeFolders");
        removeNodeFolders(path);
        System.out.println("--> Compile plugin log in " + path);
        File compilePomLogfile = new File(path + "/compilePluginLog.log");
        runner.run(mavenConfig, path, compilePomLogfile, "clean", "process-test-classes", "-Dmaven.javadoc.skip");
    }

    private void removeNodeFolders(File path) throws IOException {
        File nodeFolder = new File(path, "node");
        if (nodeFolder.exists() && nodeFolder.isDirectory()) {
            System.out.println("--> node folder exists, deleting");
            FileUtils.deleteDirectory(nodeFolder);
        }
        File nodeModulesFolder = new File(path, "node_modules");
        if (nodeModulesFolder.exists() && nodeModulesFolder.isDirectory()) {
            System.out.println("--> node folder exists, deleting");
            FileUtils.deleteDirectory(nodeModulesFolder);
        }
    }

    private MavenRunner.Config getMavenConfig(PluginCompatTesterConfig config) throws IOException {
        MavenRunner.Config mconfig = new MavenRunner.Config();
        mconfig.userSettingsFile = config.getM2SettingsFile();
        mconfig.userProperties.putAll(config.retrieveMavenProperties());
        return mconfig;
    }
}
