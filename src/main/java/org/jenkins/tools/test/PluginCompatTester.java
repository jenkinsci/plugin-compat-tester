/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkins.tools.test;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.gradle.ExternalGradleRunner;
import org.jenkins.tools.test.maven.ExpressionEvaluator;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.BeforeCompilationContext;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.model.plugin_metadata.LocalCheckoutPluginMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;
import org.jenkins.tools.test.util.*;

/**
 * Frontend for plugin compatibility tests
 *
 * @author Frederic Camblor, Olivier Lamy
 */
@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
public class PluginCompatTester {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTester.class.getName());

    /**
     * A sentinel value for the Git URL to be used for local checkouts.
     */
    private static final String LOCAL_CHECKOUT = "<local checkout>";

    private final PluginCompatTesterConfig config;
    private final ExternalMavenRunner runner;
    private final ExternalGradleRunner gradleRunner;

    public PluginCompatTester(PluginCompatTesterConfig config) {
        this.config = config;
        runner = new ExternalMavenRunner(config);
        gradleRunner = new ExternalGradleRunner(config);
    }

    @SuppressFBWarnings(
            value = "UNSAFE_HASH_EQUALS",
            justification = "We are not used Git SHA comparisons for security")
    public void testPlugins() throws PluginCompatibilityTesterException {
        ServiceHelper serviceHelper = new ServiceHelper(config.getExternalHooksJars());
        PluginCompatTesterHooks pcth = new PluginCompatTesterHooks(serviceHelper, config.getExcludeHooks());

        // Extract the metadata
        WarExtractor warExtractor = new WarExtractor(
                config.getWar(), serviceHelper, config.getIncludePlugins(), config.getExcludePlugins());
        String coreVersion = warExtractor.extractCoreVersion();

        NavigableMap<String, List<Plugin>> pluginsByRepository;

        if (localCheckoutProvided()) {
            // if a user provides a local checkout we do not also check anything in the way.
            LocalCheckoutPluginMetadataExtractor localCheckoutPluginMetadataExtractor =
                    new LocalCheckoutPluginMetadataExtractor(config, runner);
            // Do not perform the before checkout hooks on a local checkout
            List<Plugin> localCheckout = localCheckoutPluginMetadataExtractor.extractMetadata();
            pluginsByRepository = new TreeMap<>(Map.of(LOCAL_CHECKOUT, localCheckout));
        } else {
            List<Plugin> plugins = warExtractor.extractPlugins();
            pluginsByRepository = WarExtractor.byRepository(plugins);

            // Sanity check all plugins in the repository come from the same hash/tag
            for (List<Plugin> pluginList : pluginsByRepository.values()) {
                Set<String> hashes = pluginList.stream().map(Plugin::getGitHash).collect(Collectors.toSet());
                if (hashes.size() != 1) {
                    throw new IllegalArgumentException("Repository "
                            + pluginList.get(0).getGitUrl()
                            + " present with multiple commits: "
                            + String.join(", ", hashes));
                }
            }

            /*
             * Run the before checkout hooks on everything that we are about to check out (as opposed to an existing local
             * checkout).
             */
            for (Plugin plugin : plugins) {
                BeforeCheckoutContext c = new BeforeCheckoutContext(coreVersion, plugin, config);
                pcth.runBeforeCheckout(c);
            }
        }

        PluginCompatibilityTesterException lastException = null;
        LOGGER.log(Level.INFO, "Starting plugin tests on core version {0}", coreVersion);

        for (Map.Entry<String, List<Plugin>> entry : pluginsByRepository.entrySet()) {
            // Construct a single working directory for the clone
            String gitUrl = entry.getKey();

            File cloneDir;
            if (gitUrl.equals(LOCAL_CHECKOUT)) {
                cloneDir = config.getLocalCheckoutDir();
            } else {
                cloneDir = new File(config.getWorkingDir(), getRepoNameFromGitUrl(gitUrl));
                // All plugins from the same reactor are from the same hash/tag
                String tag = entry.getValue().get(0).getGitHash();

                try {
                    cloneFromScm(gitUrl, config.getFallbackGitHubOrganization(), tag, cloneDir);
                } catch (PluginSourcesUnavailableException e) {
                    lastException = throwOrAddSuppressed(lastException, e, config.isFailFast());
                    LOGGER.log(
                            Level.SEVERE,
                            String.format("Internal error while cloning repository %s at commit %s.", gitUrl, tag),
                            e);
                    continue;
                }
            }

            BuildSystem buildSystem = BuildSystemUtils.detectBuildSystem(cloneDir);
            LOGGER.log(Level.INFO, "Detected build system: {0} for repository {1}", new Object[] {buildSystem, gitUrl});

            if (!config.isCompileOnly()) {
                // For each of the plugin metadata entries, go test the plugin
                for (Plugin plugin : entry.getValue()) {
                    try {
                        testPluginAgainst(coreVersion, plugin, cloneDir, pcth, buildSystem);
                    } catch (PluginCompatibilityTesterException e) {
                        lastException = throwOrAddSuppressed(lastException, e, config.isFailFast());
                        LOGGER.log(
                                Level.SEVERE,
                                String.format(
                                        "Internal error while executing a test for core %s and plugin %s at version %s.",
                                        coreVersion, plugin.getName(), plugin.getVersion()),
                                e);
                    }
                }
            } else {
                try {
                    testCompilationAgainst(coreVersion, gitUrl, cloneDir, buildSystem);
                } catch (PluginCompatibilityTesterException e) {
                    lastException = throwOrAddSuppressed(lastException, e, config.isFailFast());
                    LOGGER.log(
                            Level.SEVERE,
                            String.format(
                                    "Internal error while executing a test for core %s and repository %s.",
                                    coreVersion, gitUrl),
                            e);
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
    }

    private static File createBuildLogFile(File workDirectory, Plugin plugin, String coreVersion) {
        File f = new File(workDirectory.getAbsolutePath()
                + File.separator
                + String.format(
                        "logs/%s/v%s_against_core_version_%s.log",
                        plugin.getPluginId(), plugin.getVersion(), coreVersion));
        createBuildLogFile(f);
        return f;
    }

    private static File createBuildLogFile(File workDirectory, String gitUrl, String coreVersion)
            throws PluginSourcesUnavailableException {
        File f = new File(workDirectory.getAbsolutePath()
                + File.separator
                + String.format("logs/%s/core_version_%s.log", getRepoNameFromGitUrl(gitUrl), coreVersion));
        createBuildLogFile(f);
        return f;
    }

    private static void createBuildLogFile(File buildLogFile) {
        try {
            Files.createDirectories(buildLogFile.getParentFile().toPath());
            Files.deleteIfExists(buildLogFile.toPath());
            Files.createFile(buildLogFile.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create build log file", e);
        }
    }

    private void testPluginAgainst(
            String coreVersion,
            Plugin plugin,
            File cloneLocation,
            PluginCompatTesterHooks pcth,
            BuildSystem buildSystem)
            throws PluginCompatibilityTesterException {
        LOGGER.log(
                Level.INFO,
                "\n\n\n\n\n\n"
                        + "#############################################\n"
                        + "#############################################\n"
                        + "##\n"
                        + "## Starting to test {0} {1} against core version {2} using {3}\n"
                        + "##\n"
                        + "#############################################\n"
                        + "#############################################\n\n\n\n\n",
                new Object[] {plugin.getName(), plugin.getVersion(), coreVersion, buildSystem.name()});

        File buildLogFile = createBuildLogFile(config.getWorkingDir(), plugin, coreVersion);

        // Run the before compile hooks
        BeforeCompilationContext beforeCompile =
                new BeforeCompilationContext(coreVersion, plugin, config, cloneLocation);
        pcth.runBeforeCompilation(beforeCompile);

        if (buildSystem == BuildSystem.GRADLE) {
            testGradlePluginAgainst(coreVersion, plugin, cloneLocation, pcth, buildLogFile);
        } else {
            testMavenPluginAgainst(coreVersion, plugin, cloneLocation, pcth, buildLogFile);
        }
    }

    private void testMavenPluginAgainst(
            String coreVersion, Plugin plugin, File cloneLocation, PluginCompatTesterHooks pcth, File buildLogFile)
            throws PluginCompatibilityTesterException {
        // First build against the original POM. This defends against source incompatibilities
        // (which we do not care about for this purpose); and ensures that we are testing a
        // plugin binary as close as possible to what was actually released. We also skip
        // potential javadoc execution to avoid general test failure.

        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("maven.javadoc.skip", "true");

        /*
         * For multi-module projects where one plugin depends on another plugin in the same multi-module project, pass
         * -Dset.changelist for incrementals releases so that Maven can find the first module when compiling the classes
         * for the second module.
         */
        boolean setChangelist = false;
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(cloneLocation, null, runner);
        if (!expressionEvaluator.evaluateList("project.modules").isEmpty()) {
            String version = expressionEvaluator.evaluateString("project.version");
            if (version.contains("999999-SNAPSHOT") && !plugin.getVersion().equals(version)) {
                setChangelist = true;
            }
        }
        if (setChangelist) {
            properties.put("set.changelist", "true");
        }
        runner.run(properties, cloneLocation, plugin.getModule(), buildLogFile, "clean", "process-test-classes");

        List<String> args = new ArrayList<>();
        args.add("hpi:resolve-test-dependencies");
        args.add("hpi:test-hpl");
        args.add("hpi:test-runtime");
        args.add("surefire:test");

        // Run preexecution hooks
        BeforeExecutionContext forExecutionHooks =
                new BeforeExecutionContext(coreVersion, plugin, config, cloneLocation, args);
        pcth.runBeforeExecution(forExecutionHooks);

        properties = new LinkedHashMap<>(config.getMavenProperties());
        properties.put("overrideWar", config.getWar().toString());
        properties.put("jenkins.version", coreVersion);
        properties.put("useUpperBounds", "true");
        if (!forExecutionHooks.getOverrideVersions().isEmpty()) {
            properties.put(
                    "overrideVersions",
                    forExecutionHooks.getOverrideVersions().entrySet().stream()
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.joining(",")));
        }
        if (!forExecutionHooks.getUpperBoundsExcludes().isEmpty()) {
            properties.put(
                    "upperBoundsExcludes",
                    forExecutionHooks.getUpperBoundsExcludes().stream().collect(Collectors.joining(",")));
        }
        if (setChangelist) {
            properties.put("set.changelist", "true");
            // As hooks may be adjusting the POMs, tell git-changelist-extension to ignore dirty commits.
            properties.put("ignore.dirt", "true");
        }

        // Execute with tests
        runner.run(
                Collections.unmodifiableMap(properties),
                cloneLocation,
                plugin.getModule(),
                buildLogFile,
                args.toArray(new String[0]));
    }

    private void testGradlePluginAgainst(
            String coreVersion, Plugin plugin, File cloneLocation, PluginCompatTesterHooks pcth, File buildLogFile)
            throws PluginCompatibilityTesterException {

        Map<String, String> properties = new LinkedHashMap<>();

        // Initial Compile
        gradleRunner.run(properties, cloneLocation, plugin.getModule(), buildLogFile);

        List<String> tasks = new ArrayList<>();

        if (!config.getGradleTasks().isEmpty()) {
            tasks.addAll(config.getGradleTasks());
        } else {
            tasks.add("test");
            tasks.add("assemble");
        }

        BeforeExecutionContext forExecutionHooks =
                new BeforeExecutionContext(coreVersion, plugin, config, cloneLocation, tasks);
        pcth.runBeforeExecution(forExecutionHooks);

        properties = new LinkedHashMap<>(config.getGradleSystemProperties());
        properties.put("cfg.plg.jenkinsVersion", coreVersion);
        properties.put("jenkins.version", coreVersion);
        properties.put("jenkinsVersion", coreVersion);

        gradleRunner.run(
                Collections.unmodifiableMap(properties),
                cloneLocation,
                plugin.getModule(),
                buildLogFile,
                tasks.toArray(new String[0]));
    }

    private void testCompilationAgainst(String coreVersion, String gitUrl, File cloneLocation, BuildSystem buildSystem)
            throws PluginCompatibilityTesterException {
        LOGGER.log(
                Level.INFO,
                "\n\n\n\n\n\n"
                        + "#############################################\n"
                        + "#############################################\n"
                        + "##\n"
                        + "## Compiling {0} against core version {1} using {2}\n"
                        + "##\n"
                        + "#############################################\n"
                        + "#############################################\n\n\n\n\n",
                new Object[] {getRepoNameFromGitUrl(gitUrl), coreVersion, buildSystem.name()});

        File buildLogFile = createBuildLogFile(config.getWorkingDir(), gitUrl, coreVersion);

        if (buildSystem == BuildSystem.GRADLE) {
            testGradleCompilationAgainst(coreVersion, cloneLocation, buildLogFile);
        } else {
            testMavenCompilationAgainst(coreVersion, cloneLocation, buildLogFile);
        }
    }

    private void testMavenCompilationAgainst(String coreVersion, File cloneLocation, File buildLogFile)
            throws PluginCompatibilityTesterException {
        Map<String, String> properties = new LinkedHashMap<>(config.getMavenProperties());
        properties.put("jenkins.version", coreVersion);
        properties.put("checkstyle.skip", "true");
        properties.put("enforcer.skip", "true");
        properties.put("invoker.skip", "true");
        properties.put("maven.javadoc.skip", "true");
        properties.put("maven.site.skip", "true");
        properties.put("skip.bower", "true");
        properties.put("skip.bun", "true");
        properties.put("skip.corepack", "true");
        properties.put("skip.ember", "true");
        properties.put("skip.grunt", "true");
        properties.put("skip.gulp", "true");
        properties.put("skip.installbun", "true");
        properties.put("skip.installnodecorepack", "true");
        properties.put("skip.installnodenpm", "true");
        properties.put("skip.installnodepnpm", "true");
        properties.put("skip.installyarn", "true");
        properties.put("skip.jspm", "true");
        properties.put("skip.karma", "true");
        properties.put("skip.npm", "true");
        properties.put("skip.npx", "true");
        properties.put("skip.pnpm", "true");
        properties.put("skip.webpack", "true");
        properties.put("skip.yarn", "true");
        properties.put("skipTests", "true");
        properties.put("spotbugs.skip", "true");
        properties.put("spotless.check.skip", "true");
        properties.put("tidy.skip", "true");

        List<String> args = new ArrayList<>();
        args.add("clean");
        args.add("verify");

        runner.run(
                Collections.unmodifiableMap(properties),
                cloneLocation,
                null,
                buildLogFile,
                args.toArray(new String[0]));
    }

    private void testGradleCompilationAgainst(String coreVersion, File cloneLocation, File buildLogFile)
            throws PluginCompatibilityTesterException {
        Map<String, String> properties = new LinkedHashMap<>(config.getGradleSystemProperties());

        properties.put("cfg.plg.jenkinsVersion", coreVersion);
        properties.put("jenkins.version", coreVersion);
        properties.put("jenkinsVersion", coreVersion);

        properties.putIfAbsent("cfg.quality.checkstyle.enabled", "false");
        properties.putIfAbsent("cfg.quality.spotbugs.enabled", "false");
        properties.putIfAbsent("cfg.quality.pmd.enabled", "false");
        properties.putIfAbsent("cfg.quality.detekt.enabled", "false");
        properties.putIfAbsent("cfg.quality.spotless.enabled", "false");
        properties.putIfAbsent("cfg.quality.owasp.enabled", "false");
        properties.putIfAbsent("cfg.quality.pitest.enabled", "false");
        properties.putIfAbsent("cfg.quality.kover.enabled", "false");
        properties.putIfAbsent("cfg.quality.eslint.enabled", "false");
        properties.putIfAbsent("cfg.quality.dokka.enabled", "false");
        properties.putIfAbsent("cfg.quality.codenarc.enabled", "false");
        properties.putIfAbsent("cfg.quality.cpd.enabled", "false");
        properties.putIfAbsent("cfg.quality.jacoco.enabled", "false");

        List<String> tasks = new ArrayList<>();
        tasks.add("clean");
        tasks.add("classes");
        tasks.add("jar");

        gradleRunner.run(
                Collections.unmodifiableMap(properties),
                cloneLocation,
                null,
                buildLogFile,
                tasks.toArray(new String[0]));
    }

    private static void cloneFromScm(
            String url, String fallbackGitHubOrganization, String scmTag, File checkoutDirectory)
            throws PluginSourcesUnavailableException {
        List<String> gitUrls = new ArrayList<>();
        gitUrls.add(url);
        if (fallbackGitHubOrganization != null) {
            gitUrls = getFallbackGitUrl(gitUrls, url, fallbackGitHubOrganization);
        }

        PluginSourcesUnavailableException lastException = null;
        for (String gitUrl : gitUrls) {
            try {
                cloneImpl(gitUrl, scmTag, checkoutDirectory);
                return; // checkout was ok
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (PluginSourcesUnavailableException e) {
                lastException = throwOrAddSuppressed(lastException, e, false);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
    }

    /**
     * Clone the given Git repository in the given checkout directory by running, in order, the
     * following CLI operations:
     *
     * <ul>
     *   <li><code>git init</code>
     *   <li><code>git remote add origin url</code>
     *   <li><code>git fetch origin ${SCM_TAG}</code>
     *   <li><code>git checkout FETCH_HEAD</code>
     * </ul>
     *
     * @param gitUrl            The git native URL, see the <a
     *                          href="https://git-scm.com/docs/git-clone#_git_urls">git documentation</a> for the
     *                          supported syntax
     * @param scmTag            the tag or sha1 hash to clone
     * @param checkoutDirectory the directory in which to clone the Git repository
     * @throws IOException if an error occurs
     */
    private static void cloneImpl(String gitUrl, String scmTag, File checkoutDirectory)
            throws IOException, PluginSourcesUnavailableException {
        LOGGER.log(Level.INFO, "Checking out from Git repository {0} at {1}", new Object[] {gitUrl, scmTag});

        /*
         * We previously used the Maven SCM API to clone the repository, which ran the following
         * commands:
         *
         *     git clone --depth 1 --branch ${SCM_TAG} ${CONNECTION_URL}
         *     git ls-remote ${CONNECTION_URL}
         *     git fetch ${CONNECTION_URL}
         *     git checkout ${SCM_TAG}
         *     git ls-files
         *
         * This proved to be inefficient, so we instead run only the commands we need to run:
         *
         *     git init
         *     git fetch ${CONNECTION_URL} ${SCM_TAG} (this will work with a SHA1 hash or a tag)
         *     git checkout FETCH_HEAD
         */
        if (checkoutDirectory.isDirectory()) {
            FileUtils.deleteDirectory(checkoutDirectory);
        }
        Files.createDirectories(checkoutDirectory.toPath());

        runCommand(checkoutDirectory, "git", "init");
        runCommand(checkoutDirectory, "git", "fetch", gitUrl, scmTag);
        runCommand(checkoutDirectory, "git", "checkout", "FETCH_HEAD");
    }

    private static List<String> getFallbackGitUrl(
            List<String> gitUrls, String gitUrlFromMetadata, String fallbackGitHubOrganization) {
        Pattern pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        Matcher matcher = pattern.matcher(gitUrlFromMetadata);
        matcher.find();
        gitUrls.add(matcher.replaceFirst("git@github.com:" + fallbackGitHubOrganization + "$3"));
        pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        matcher = pattern.matcher(gitUrlFromMetadata);
        matcher.find();
        gitUrls.add(matcher.replaceFirst("$1" + fallbackGitHubOrganization + "$3"));
        return gitUrls;
    }

    private boolean localCheckoutProvided() {
        File localCheckoutDir = config.getLocalCheckoutDir();
        return localCheckoutDir != null && localCheckoutDir.exists();
    }

    public static String getRepoNameFromGitUrl(String gitUrl) throws PluginSourcesUnavailableException {
        // obtain the last path component (and strip any trailing .git)
        int index = gitUrl.lastIndexOf("/");
        if (index < 0) {
            throw new PluginSourcesUnavailableException("Failed to obtain local directory for " + gitUrl);
        }
        String name = gitUrl.substring(++index);
        if (name.endsWith(".git")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    /**
     * Throws {@code current} if {@code throwException} is {@code true} or returns {@code caught}
     * with {@code current} added (if non-null) as a suppressed exception.
     *
     * @param <T>
     * @param current        the PluginCompatibilityTesterException if any
     * @param caught         the newly caught exception
     * @param throwException {@code true} if we should immediately rethrow {@code caught}, {@code
     *                       false} indicating we should return {@caught}.
     * @return {@code caught}
     * @throws PluginCompatibilityTesterException if {@code throwException == true} then {@caught}
     *                                            is thrown.
     */
    private static <T extends PluginCompatibilityTesterException> T throwOrAddSuppressed(
            @CheckForNull PluginCompatibilityTesterException current, T caught, boolean throwException) throws T {
        if (throwException) {
            throw caught;
        }
        if (current != null) {
            caught.addSuppressed(current);
        }
        return caught;
    }

    /**
     * Runs the given command, waiting until it has completed before returning.
     *
     * @param directory      the directory to run the command in.
     * @param commandAndArgs the command and arguments to run.
     * @throws IOException                       if the process could not be started.
     * @throws PluginSourcesUnavailableException if the command failed (either it was interrupted or exited with a non zero status.
     */
    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "intended behaviour")
    private static void runCommand(File directory, String... commandAndArgs)
            throws IOException, PluginSourcesUnavailableException {
        Process p = new ProcessBuilder()
                .directory(directory)
                .command(commandAndArgs)
                .redirectErrorStream(true)
                .start();
        StreamGobbler gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new PluginSourcesUnavailableException(
                        String.join(" ", commandAndArgs) + " failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new PluginSourcesUnavailableException(String.join(" ", commandAndArgs) + " was interrupted", e);
        }
    }
}
