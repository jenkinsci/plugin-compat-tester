package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHook;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;

public class MultiParentCompileHook extends PluginCompatTesterHookBeforeCompile {

    private static final Logger LOGGER = Logger.getLogger(MultiParentCompileHook.class.getName());

    protected MavenRunner runner;
    protected MavenRunner.Config mavenConfig;

    public static final String ESLINTRC = ".eslintrc";

    public MultiParentCompileHook() {
        LOGGER.log(Level.INFO, "Loaded multi-parent compile hook");
    }

    @Override
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "silly rule")
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        try {
            LOGGER.log(Level.INFO, "Executing multi-parent compile hook");
            PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");

            runner = new ExternalMavenRunner(config.getExternalMaven());
            mavenConfig = getMavenConfig(config);

            File pluginDir = (File) moreInfo.get("pluginDir");
            LOGGER.log(Level.INFO, "Plugin dir is {0}", pluginDir);

            File localCheckoutDir = config.getLocalCheckoutDir();
            if (localCheckoutDir != null) {
                Path pluginSourcesDir = localCheckoutDir.toPath();
                boolean isMultipleLocalPlugins = config.getIncludePlugins() != null && config.getIncludePlugins().size() > 1;
                // We are running for local changes, let's copy the .eslintrc file if we can
                // If we are using localCheckoutDir with multiple plugins the .eslintrc must be located at the top level
                // If not it must be located on the parent of the localCheckoutDir
                if (!isMultipleLocalPlugins) {
                    pluginSourcesDir = pluginSourcesDir.getParent();
                }
                // Copy the file if it exists
                try (Stream<Path> walk = Files.walk(pluginSourcesDir, 1)) {
                    walk.filter(this::isEslintFile).forEach(eslintrc -> copy(eslintrc, pluginDir));
                }
            }

            // We need to compile before generating effective pom overriding jenkins.version
            // only if the plugin is not already compiled
            boolean ranCompile = moreInfo.containsKey(OVERRIDE_DEFAULT_COMPILE) && (boolean) moreInfo.get(OVERRIDE_DEFAULT_COMPILE);
            if (!ranCompile) {
                compile(mavenConfig, pluginDir, localCheckoutDir, (String) moreInfo.get("parentFolder"), (String) moreInfo.get("pluginName"));
                moreInfo.put(OVERRIDE_DEFAULT_COMPILE, true);
            }

            LOGGER.log(Level.INFO, "Executed multi-parent compile hook");
            return moreInfo;
            // Exceptions get swallowed, so we print to console here and rethrow again
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception executing hook", e);
            throw e;
        }
    }

    @Override
    public void validate(Map<String, Object> toCheck) {

    }

    @Override
    public boolean check(Map<String, Object> info) {
        for (PluginCompatTesterHook hook : PluginCompatTesterHooks.getHooksFromStage("checkout", info)) {
            if (hook instanceof AbstractMultiParentHook && hook.check(info)) {
                return true;
            }
        }
        return false;
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
        MavenRunner.Config mconfig = new MavenRunner.Config(config);
        // TODO REMOVE
        mconfig.userProperties.put("failIfNoTests", "false");
        return mconfig;
    }

    private void compile(MavenRunner.Config mavenConfig, File path, File localCheckoutDir, String parentFolder, String pluginName) throws PomExecutionException, IOException {
        if (isSnapshotMultiParentPlugin(parentFolder, path, localCheckoutDir)) {
            // "process-test-classes" not working properly on multi-module plugin. See https://issues.jenkins.io/browse/JENKINS-62658
            // installs dependencies into local repository
            String mavenModule = PluginCompatTester.getMavenModule(pluginName, path, runner, mavenConfig);
            if (mavenModule == null || mavenModule.isBlank()) {
                throw new IOException(String.format("Unable to retrieve the Maven module for plugin %s on %s", pluginName, path));
            }
            runner.run(mavenConfig, path.getParentFile(), setupCompileResources(path.getParentFile()), "clean", "install", "-DskipTests", "-Dinvoker.skip", "-Denforcer.skip", "-Dmaven.javadoc.skip", "-am", "-pl", mavenModule);
        } else {
            runner.run(mavenConfig, path, setupCompileResources(path), "clean", "process-test-classes", "-Dmaven.javadoc.skip");
        }
    }

    /**
     * Checks if a plugin is a multiparent plugin with a SNAPSHOT project.version and
     * without local checkout directory overriden.
     */
    private boolean isSnapshotMultiParentPlugin(String parentFolder, File path, File localCheckoutDir) throws PomExecutionException, IOException {
        if (localCheckoutDir != null) {
            return false;
        }
        if (parentFolder == null || parentFolder.isBlank()) {
            return false;
        }
        if (!path.getAbsolutePath().contains(parentFolder)) {
            LOGGER.log(Level.WARNING, "Parent folder {0} not present in path {1}", new Object[]{parentFolder, path.getAbsolutePath()});
            return false;
        }
        File parentFile = path.getParentFile();
        if (!StringUtils.equals(parentFolder, parentFile.getName())) {
            LOGGER.log(Level.WARNING, "{0} is not the parent folder of {1}", new Object[]{parentFolder, path.getAbsolutePath()});
            return false;
        }

        File log = new File(parentFile.getAbsolutePath() + File.separatorChar + "version.log");
        runner.run(mavenConfig, parentFile, log, "-Dexpression=project.version", "-q", "-DforceStdout", "help:evaluate");
        List<String> output = Files.readAllLines(log.toPath(), Charset.defaultCharset());
        return output.get(output.size() - 1).endsWith("-SNAPSHOT");
    }

    private File setupCompileResources(File path) throws IOException {
        LOGGER.log(Level.INFO, "Cleaning up node modules if necessary");
        removeNodeFolders(path);
        LOGGER.log(Level.INFO, "Plugin compilation log directory: {0}", path);
        return new File(path + "/compilePluginLog.log");
    }

    private void removeNodeFolders(File path) throws IOException {
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
