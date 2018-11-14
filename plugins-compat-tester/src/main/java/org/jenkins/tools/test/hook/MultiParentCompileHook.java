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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

public class MultiParentCompileHook extends PluginCompatTesterHookBeforeCompile {

    protected MavenRunner runner;
    protected MavenRunner.Config mavenConfig;

    public static final String ESLINTRC = ".eslintrc";

    public MultiParentCompileHook() {
        System.out.println("Loaded multi-parent compile hook");
    }


    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        try {
            System.out.println("Executing multi-parent compile hook");
            PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
            MavenCoordinates core = (MavenCoordinates) moreInfo.get("core");

            runner = config.getExternalMaven() == null ? new InternalMavenRunner() : new ExternalMavenRunner(config.getExternalMaven());
            mavenConfig = getMavenConfig(config);

            File pluginDir = (File) moreInfo.get("pluginDir");
            System.out.println("Plugin dir is " + pluginDir);

            if (config.getLocalCheckoutDir() != null) {
                Path pluginSourcesDir = config.getLocalCheckoutDir().toPath();
                boolean isMultipleLocalPlugins = config.getIncludePlugins() != null && config.getIncludePlugins().size() > 1;
                // We are running for local changes, let's copy the .eslintrc file if we can
                // If we are using localCheckoutDir with multiple plugins the .eslintrc must be located at the top level
                // If not it must be located on the parent of the localCheckoutDir
                if (!isMultipleLocalPlugins) {
                    pluginSourcesDir = pluginSourcesDir.getParent();
                }
                // Copy the file if it exists
                Files.walk(pluginSourcesDir, 1)
                    .filter(this::isEslintFile)
                    .forEach(eslintrc -> copy(eslintrc, pluginDir));
            }

            // We need to compile before generating effective pom overriding jenkins.version
            // only if the plugin is not already compiled
            boolean ranCompile = moreInfo.containsKey(OVERRIDE_DEFAULT_COMPILE) ? (boolean) moreInfo.get(OVERRIDE_DEFAULT_COMPILE) : false;
            if (!ranCompile) {
                compile(mavenConfig, pluginDir);
                moreInfo.put(OVERRIDE_DEFAULT_COMPILE, true);
            }

            System.out.println("Executed multi-parent compile hook");
            return moreInfo;
            // Exceptions get swallowed, so we print to console here and rethrow again
        } catch (Exception e) {
            System.out.println("Exception executing hook");
            System.out.println(e);
            throw e;
        }
    }

    @Override
    public void validate(Map<String, Object> toCheck) throws Exception {

    }

    @Override
    public boolean check(Map<String, Object> info) throws Exception {
        return BlueOceanHook.isBOPlugin(info) || DeclarativePipelineHook.isDPPlugin(info) || StructsHook.isStructsPlugin(info);
    }

    private boolean isEslintFile(Path file) {
        return file.getFileName().toString().equals(ESLINTRC);
    }

    private void copy(Path eslintrc, File pluginFolder) {
        try {
            Files.copy(eslintrc, new File(pluginFolder.getParent(), ESLINTRC).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy eslintrc file", e);
        }
    }

    private MavenRunner.Config getMavenConfig(PluginCompatTesterConfig config) throws IOException {
        MavenRunner.Config mconfig = new MavenRunner.Config();
        mconfig.userSettingsFile = config.getM2SettingsFile();
        // TODO REMOVE
        mconfig.userProperties.put("failIfNoTests", "false");
        mconfig.userProperties.putAll(config.retrieveMavenProperties());

        return mconfig;
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
