package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.BeforeCompilationContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHook;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.model.hook.Stage;
import org.jenkins.tools.test.model.hook.StageContext;

@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
public class MultiParentCompileHook extends PluginCompatTesterHookBeforeCompile {

    private static final Logger LOGGER = Logger.getLogger(MultiParentCompileHook.class.getName());

    protected MavenRunner runner;

    public MultiParentCompileHook() {
        LOGGER.log(Level.INFO, "Loaded multi-parent compile hook");
    }

    @Override
    public void action(@NonNull BeforeCompilationContext context) throws PomExecutionException {
        LOGGER.log(Level.INFO, "Executing multi-parent compile hook");
        PluginCompatTesterConfig config = context.getConfig();

        runner =
                new ExternalMavenRunner(
                        config.getExternalMaven(),
                        config.getMavenSettings(),
                        config.getMavenArgs());

        File pluginDir = context.getPluginDir();
        LOGGER.log(Level.INFO, "Plugin dir is {0}", pluginDir);

        // We need to compile before generating effective pom overriding jenkins.version
        // only if the plugin is not already compiled
        if (!context.ranCompile()) {
            compile(
                    pluginDir,
                    config.getLocalCheckoutDir(),
                    context.getParentFolder(),
                    context.getPlugin().name);
            context.setRanCompile(true);
        }

        LOGGER.log(Level.INFO, "Executed multi-parent compile hook");
    }

    @Override
    public boolean check(@NonNull BeforeCompilationContext context) {
        for (PluginCompatTesterHook<? extends StageContext> hook :
                PluginCompatTesterHooks.hooksByStage.get(Stage.CHECKOUT)) {
            PluginCompatTesterHook<BeforeCheckoutContext> checkoutHook =
                    (PluginCompatTesterHook<BeforeCheckoutContext>) hook;
            if (checkoutHook instanceof AbstractMultiParentHook
                    && checkoutHook.check(
                            new BeforeCheckoutContext(
                                    context.getPlugin(),
                                    context.getModel(),
                                    context.getCoreCoordinates(),
                                    context.getConfig()))) {
                return true;
            }
        }
        return false;
    }

    private void compile(File path, File localCheckoutDir, String parentFolder, String pluginName)
            throws PomExecutionException {
        if (isSnapshotMultiParentPlugin(parentFolder, path, localCheckoutDir)) {
            // "process-test-classes" not working properly on multi-module plugin.
            // See https://issues.jenkins.io/browse/JENKINS-62658
            // installs dependencies into local repository
            String mavenModule = PluginCompatTester.getMavenModule(pluginName, path, runner);
            if (mavenModule == null || mavenModule.isBlank()) {
                throw new IllegalStateException(
                        String.format(
                                "Unable to retrieve the Maven module for plugin %s on %s",
                                pluginName, path));
            }
            runner.run(
                    Map.of(
                            "skipTests",
                            "true",
                            "invoker.skip",
                            "true",
                            "enforcer.skip",
                            "true",
                            "maven.javadoc.skip",
                            "true"),
                    path.getParentFile(),
                    setupCompileResources(path.getParentFile()),
                    "clean",
                    "install",
                    "-am",
                    "-pl",
                    mavenModule);
        } else {
            runner.run(
                    Map.of("maven.javadoc.skip", "true"),
                    path,
                    setupCompileResources(path),
                    "clean",
                    "process-test-classes");
        }
    }

    /**
     * Checks if a plugin is a multiparent plugin with a SNAPSHOT project.version and without local
     * checkout directory overriden.
     */
    private boolean isSnapshotMultiParentPlugin(
            String parentFolder, File path, File localCheckoutDir) throws PomExecutionException {
        if (localCheckoutDir != null) {
            return false;
        }
        if (parentFolder == null || parentFolder.isBlank()) {
            return false;
        }
        if (!path.getAbsolutePath().contains(parentFolder)) {
            LOGGER.log(
                    Level.WARNING,
                    "Parent folder {0} not present in path {1}",
                    new Object[] {parentFolder, path.getAbsolutePath()});
            return false;
        }
        File parentFile = path.getParentFile();
        if (!StringUtils.equals(parentFolder, parentFile.getName())) {
            LOGGER.log(
                    Level.WARNING,
                    "{0} is not the parent folder of {1}",
                    new Object[] {parentFolder, path.getAbsolutePath()});
            return false;
        }

        File log = new File(parentFile.getAbsolutePath() + File.separatorChar + "version.log");
        runner.run(
                Map.of("expression", "project.version", "output", log.getAbsolutePath()),
                parentFile,
                null,
                "-q",
                "help:evaluate");
        List<String> output;
        try {
            output = Files.readAllLines(log.toPath(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.get(output.size() - 1).endsWith("-SNAPSHOT");
    }

    private File setupCompileResources(File path) {
        LOGGER.log(Level.INFO, "Plugin compilation log directory: {0}", path);
        return new File(path + "/compilePluginLog.log");
    }
}
