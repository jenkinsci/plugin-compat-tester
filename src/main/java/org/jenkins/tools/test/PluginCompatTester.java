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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.model.CheckoutInfo;
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PluginRemoting;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.BeforeCompilationContext;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.util.StreamGobbler;

/**
 * Frontend for plugin compatibility tests
 *
 * @author Frederic Camblor, Olivier Lamy
 */
@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
public class PluginCompatTester {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTester.class.getName());

    /** First version with new parent POM. */
    public static final String JENKINS_CORE_FILE_REGEX =
            "WEB-INF/lib/jenkins-core-([0-9.]+(?:-[0-9a-f.]+)*(?:-(?i)([a-z]+)(-)?([0-9a-f.]+)?)?(?:-(?i)([a-z]+)(-)?([0-9a-f_.]+)?)?(?:-SNAPSHOT)?)[.]jar";

    private final PluginCompatTesterConfig config;
    private final ExternalMavenRunner runner;

    public PluginCompatTester(PluginCompatTesterConfig config) {
        this.config = config;
        runner =
                new ExternalMavenRunner(
                        config.getExternalMaven(),
                        config.getMavenSettings(),
                        config.getMavenArgs());
    }

    public void testPlugins() throws PluginCompatibilityTesterException {
        PluginCompatTesterHooks pcth =
                new PluginCompatTesterHooks(
                        config.getExternalHooksJars(), config.getExcludeHooks());
        // Determine the plugin data

        // Scan bundled plugins. If there is any bundled plugin, only these plugins will be taken
        // under the consideration for the PCT run.
        UpdateSite.Data data =
                scanWAR(config.getWar(), "WEB-INF/(?:optional-)?plugins/([^/.]+)[.][hj]pi");
        if (!data.plugins.isEmpty()) {
            // Scan detached plugins to recover proper Group IDs for them. At the moment, we are
            // considering that bomfile contains the info about the detached ones.
            UpdateSite.Data detachedData =
                    scanWAR(config.getWar(), "WEB-INF/(?:detached-)?plugins/([^/.]+)[.][hj]pi");

            // Add detached if and only if no added as normal one
            if (detachedData != null) {
                detachedData.plugins.forEach(
                        (key, value) -> {
                            if (!data.plugins.containsKey(key)) {
                                data.plugins.put(key, value);
                            }
                        });
            }
        }

        if (data.plugins.isEmpty()) {
            throw new PluginCompatibilityTesterException(
                    "List of plugins to check is empty, it is not possible to run PCT");
        }

        // if there is only one plugin and it's not already resolved (not in the war) and there is a
        // local checkout available then it needs to be added to the plugins to check
        if (onlyOnePluginIncluded()
                && localCheckoutProvided()
                && !data.plugins.containsKey(config.getIncludePlugins().get(0))) {
            String artifactId = config.getIncludePlugins().get(0);
            UpdateSite.Plugin extracted = extractFromLocalCheckout();
            data.plugins.put(artifactId, extracted);
        }

        Dependency coreCoordinates = new Dependency();
        coreCoordinates.setGroupId("org.jenkins-ci.main");
        coreCoordinates.setArtifactId("jenkins-war");
        coreCoordinates.setVersion(data.core.version);
        coreCoordinates.setType("executable-war");

        PluginCompatibilityTesterException lastException = null;
        LOGGER.log(Level.INFO, "Starting plugin tests on core coordinates {0}", coreCoordinates);
        Set<CheckoutInfo> checkoutInfos = new LinkedHashSet<>();
        for (UpdateSite.Plugin plugin : data.plugins.values()) {
            if (!config.getIncludePlugins().isEmpty()
                    && !config.getIncludePlugins().contains(plugin.name.toLowerCase())) {
                LOGGER.log(Level.FINE, "Plugin {0} not in included plugins; skipping", plugin.name);
                continue;
            }

            if (!config.getExcludePlugins().isEmpty()
                    && config.getExcludePlugins().contains(plugin.name.toLowerCase())) {
                LOGGER.log(Level.INFO, "Plugin {0} in excluded plugins; skipping", plugin.name);
                continue;
            }

            PluginRemoting remote;
            if (localCheckoutProvided() && onlyOnePluginIncluded()) {
                // Only one plugin and checkout directory provided
                remote = new PluginRemoting(new File(config.getLocalCheckoutDir(), "pom.xml"));
            } else if (localCheckoutProvided()) {
                // Local directory provided for more than one plugin, so each plugin is allocated in
                // localCheckoutDir/plugin-name. If there is no subdirectory for the plugin, it will
                // be cloned from SCM.
                File pomFile =
                        new File(new File(config.getLocalCheckoutDir(), plugin.name), "pom.xml");
                if (pomFile.exists()) {
                    remote = new PluginRemoting(pomFile);
                } else {
                    remote = new PluginRemoting(plugin.url);
                }
            } else {
                // Only one plugin but checkout directory not provided or more than a plugin and no
                // local checkout directory provided
                remote = new PluginRemoting(plugin.url);
            }
            Model model = remote.retrieveModel();
            checkoutInfos.add(
                    new CheckoutInfo(model.getScm().getConnection(), model.getScm().getTag()));
        }

        Map<String, Set<String>> repoToTag = new LinkedHashMap<>();
        for (CheckoutInfo checkoutInfo : checkoutInfos) {
            repoToTag
                    .computeIfAbsent(checkoutInfo.getConnection(), unused -> new LinkedHashSet<>())
                    .add(checkoutInfo.getTag());
        }
        for (Map.Entry<String, Set<String>> entry : repoToTag.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new IllegalArgumentException(
                        "Repository "
                                + entry.getKey()
                                + " present with multiple tags: "
                                + String.join(", ", entry.getValue()));
            }
        }

        for (CheckoutInfo checkoutInfo : checkoutInfos) {
            try {
                testRepositoryAgainst(coreCoordinates, checkoutInfo, pcth);
            } catch (PluginCompatibilityTesterException e) {
                if (lastException != null) {
                    e.addSuppressed(lastException);
                }
                lastException = e;
                if (config.isFailFast()) {
                    break;
                } else {
                    LOGGER.log(
                            Level.SEVERE,
                            String.format(
                                    "Internal error while executing a test for core %s and repository %s at tag %s.",
                                    coreCoordinates.getVersion(),
                                    checkoutInfo.getConnection(),
                                    checkoutInfo.getTag()),
                            e);
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    private UpdateSite.Plugin extractFromLocalCheckout() {
        Model model =
                new PluginRemoting(new File(config.getLocalCheckoutDir(), "pom.xml"))
                        .retrieveModel();
        return new UpdateSite.Plugin(
                model.getArtifactId(),
                "" /* version is not required */,
                model.getScm().getConnection(),
                null);
    }

    private static File createBuildLogFile(
            File workDirectory, String pluginName, String pluginVersion, Dependency coreCoords) {
        return new File(
                workDirectory.getAbsolutePath()
                        + File.separator
                        + createBuildLogFilePathFor(pluginName, pluginVersion, coreCoords));
    }

    private static String createBuildLogFilePathFor(
            String pluginName, String pluginVersion, Dependency coreCoords) {
        return String.format(
                "logs/%s/v%s_against_%s_%s_%s.log",
                pluginName,
                pluginVersion,
                coreCoords.getGroupId(),
                coreCoords.getArtifactId(),
                coreCoords.getVersion());
    }

    private void testRepositoryAgainst(
            Dependency coreCoordinates, CheckoutInfo checkoutInfo, PluginCompatTesterHooks pcth)
            throws PluginCompatibilityTesterException {
        URI repositoryUri =
                URI.create(StringUtils.substringAfter(checkoutInfo.getConnection(), "scm:git:"));
        Path repositoryPath = Paths.get(repositoryUri.getPath()).getFileName();
        if (repositoryPath == null) {
            throw new AssertionError("Could never happen, but needed to silence SpotBugs");
        }
        String repositoryName = StringUtils.substringBefore(repositoryPath.toString(), ".git");
        LOGGER.log(
                Level.INFO,
                "\n\n\n\n\n\n"
                        + "#############################################\n"
                        + "#############################################\n"
                        + "##\n"
                        + "## Starting to test {0} {1} against {2}\n"
                        + "##\n"
                        + "#############################################\n"
                        + "#############################################\n\n\n\n\n",
                new Object[] {repositoryName, checkoutInfo.getTag(), coreCoordinates});

        File pluginCheckoutDir =
                new File(
                        config.getWorkingDir().getAbsolutePath()
                                + File.separator
                                + repositoryName
                                + File.separator);

        // Run any precheckout hooks
        BeforeCheckoutContext beforeCheckout = new BeforeCheckoutContext(coreCoordinates, config);
        pcth.runBeforeCheckout(beforeCheckout);

        try {
            if (Files.isDirectory(pluginCheckoutDir.toPath())) {
                LOGGER.log(
                        Level.INFO,
                        "Deleting working directory {0}",
                        pluginCheckoutDir.getAbsolutePath());
                FileUtils.deleteDirectory(pluginCheckoutDir);
            }

            Files.createDirectory(pluginCheckoutDir.toPath());
            LOGGER.log(
                    Level.INFO,
                    "Created plugin checkout directory {0}",
                    pluginCheckoutDir.getAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (localCheckoutProvided()) {
            if (!onlyOnePluginIncluded()) {
                File localCheckoutPluginDir =
                        new File(config.getLocalCheckoutDir(), repositoryName);
                File pomLocalCheckoutPluginDir = new File(localCheckoutPluginDir, "pom.xml");
                if (pomLocalCheckoutPluginDir.exists()) {
                    LOGGER.log(
                            Level.INFO,
                            "Copying plugin directory from {0}",
                            localCheckoutPluginDir.getAbsolutePath());
                    try {
                        org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(
                                localCheckoutPluginDir, pluginCheckoutDir);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else {
                    cloneFromScm(
                            checkoutInfo.getConnection(),
                            config.getFallbackGitHubOrganization(),
                            checkoutInfo.getTag(),
                            pluginCheckoutDir);
                }
            } else {
                // TODO this fails when it encounters symlinks (e.g.
                // work/jobs/…/builds/lastUnstableBuild), and even up-to-date versions of
                // org.apache.commons.io.FileUtils seem to not handle links, so may need to
                // use something like
                // http://docs.oracle.com/javase/tutorial/displayCode.html?code=http://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
                File localCheckoutDir = config.getLocalCheckoutDir();
                if (localCheckoutDir == null) {
                    throw new AssertionError("Could never happen, but needed to silence SpotBugs");
                }
                LOGGER.log(
                        Level.INFO,
                        "Copy plugin directory from {0}",
                        localCheckoutDir.getAbsolutePath());
                try {
                    org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(
                            localCheckoutDir, pluginCheckoutDir);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } else {
            // These hooks could redirect the SCM, skip checkout (if multiple plugins use
            // the same preloaded repo)
            cloneFromScm(
                    checkoutInfo.getConnection(),
                    config.getFallbackGitHubOrganization(),
                    checkoutInfo.getTag(),
                    pluginCheckoutDir);
        }

        PluginRemoting remote = new PluginRemoting(new File(pluginCheckoutDir, "pom.xml"));
        Model model = remote.retrieveModel();

        File buildLogFile =
                createBuildLogFile(
                        config.getWorkingDir(),
                        repositoryName,
                        checkoutInfo.getTag(),
                        coreCoordinates);
        try {
            FileUtils.forceMkdir(buildLogFile.getParentFile()); // Creating log directory
            FileUtils.touch(buildLogFile); // Creating log file
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Ran the BeforeCompileHooks
        BeforeCompilationContext beforeCompile =
                new BeforeCompilationContext(coreCoordinates, config, model);
        pcth.runBeforeCompilation(beforeCompile);

        // First build against the original POM. This defends against source incompatibilities
        // (which we do not care about for this purpose); and ensures that we are testing a
        // plugin binary as close as possible to what was actually released. We also skip
        // potential javadoc execution to avoid general test failure.
        runner.run(
                Map.of("maven.javadoc.skip", "true"),
                pluginCheckoutDir,
                buildLogFile,
                "clean",
                "package",
                "-Pquick-build");

        List<String> args = new ArrayList<>();
        args.add("test");

        // Run preexecution hooks
        BeforeExecutionContext forExecutionHooks =
                new BeforeExecutionContext(
                        coreCoordinates,
                        config,
                        model,
                        pluginCheckoutDir,
                        args,
                        new MavenPom(pluginCheckoutDir));
        pcth.runBeforeExecution(forExecutionHooks);

        Map<String, String> properties = new LinkedHashMap<>(config.getMavenProperties());
        properties.put("overrideWar", config.getWar().toString());
        properties.put("jenkins.version", coreCoordinates.getVersion());
        properties.put("useUpperBounds", "true");
        if (new VersionNumber(coreCoordinates.getVersion())
                .isOlderThan(new VersionNumber("2.382"))) {
            /*
             * Versions of Jenkins prior to 2.382 are susceptible to JENKINS-68696, in which
             * javax.servlet:servlet-api comes from core at version 0. This is an intentional trick
             * to prevent this library from being used, and we do not want it to be upgraded to a
             * nonzero version (which is not a realistic test scenario) just because it happens to
             * be on the class path of some plugin and triggers an upper bounds violation.
             */
            properties.put("upperBoundsExcludes", "javax.servlet:servlet-api");
        }

        // Execute with tests
        runner.run(
                Collections.unmodifiableMap(properties),
                pluginCheckoutDir,
                buildLogFile,
                args.toArray(new String[0]));
    }

    private static void cloneFromScm(
            String url, String fallbackGitHubOrganization, String scmTag, File checkoutDirectory)
            throws PluginSourcesUnavailableException {
        List<String> connectionURLs = new ArrayList<>();
        connectionURLs.add(url);
        if (fallbackGitHubOrganization != null) {
            connectionURLs =
                    getFallbackConnectionURL(connectionURLs, url, fallbackGitHubOrganization);
        }

        PluginSourcesUnavailableException lastException = null;
        for (String connectionURL : connectionURLs) {
            if (connectionURL != null) {
                if (StringUtils.startsWith(connectionURL, "scm:git:")) {
                    connectionURL = StringUtils.substringAfter(connectionURL, "scm:git:");
                }
                // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/
                connectionURL = connectionURL.replace("git://", "https://");
            }
            try {
                cloneImpl(connectionURL, scmTag, checkoutDirectory);
                return; // checkout was ok
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (PluginSourcesUnavailableException e) {
                if (lastException != null) {
                    e.addSuppressed(lastException);
                }
                lastException = e;
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
     * @param gitUrl The git native URL, see the <a
     *     href="https://git-scm.com/docs/git-clone#_git_urls">git documentation</a> for the
     *     supported syntax
     * @param scmTag the tag or sha1 hash to clone
     * @param checkoutDirectory the directory in which to clone the Git repository
     * @throws IOException if an error occurs
     */
    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "intended behavior")
    private static void cloneImpl(String gitUrl, String scmTag, File checkoutDirectory)
            throws IOException, PluginSourcesUnavailableException {
        LOGGER.log(
                Level.INFO,
                "Checking out from git repository {0} at {1}",
                new Object[] {gitUrl, scmTag});

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

        // git init
        Process p =
                new ProcessBuilder()
                        .directory(checkoutDirectory)
                        .command("git", "init")
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
                        "git init failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new PluginSourcesUnavailableException("git init was interrupted", e);
        }

        p =
                new ProcessBuilder()
                        .directory(checkoutDirectory)
                        .command("git", "fetch", gitUrl, scmTag)
                        .redirectErrorStream(true)
                        .start();
        gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new PluginSourcesUnavailableException(
                        "git fetch origin failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new PluginSourcesUnavailableException("git fetch origin was interrupted", e);
        }

        // git checkout FETCH_HEAD
        p =
                new ProcessBuilder()
                        .directory(checkoutDirectory)
                        .command("git", "checkout", "FETCH_HEAD")
                        .redirectErrorStream(true)
                        .start();
        gobbler = new StreamGobbler(p.getInputStream());
        gobbler.start();
        try {
            int exitStatus = p.waitFor();
            gobbler.join();
            String output = gobbler.getOutput().trim();
            if (exitStatus != 0) {
                throw new PluginSourcesUnavailableException(
                        "git checkout FETCH_HEAD failed with exit status "
                                + exitStatus
                                + ": "
                                + output);
            }
        } catch (InterruptedException e) {
            throw new PluginSourcesUnavailableException(
                    "git checkout FETCH_HEAD was interrupted", e);
        }
    }

    public static List<String> getFallbackConnectionURL(
            List<String> connectionURLs,
            String connectionURLPomData,
            String fallbackGitHubOrganization) {
        Pattern pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        Matcher matcher = pattern.matcher(connectionURLPomData);
        matcher.find();
        connectionURLs.add(
                matcher.replaceFirst(
                        "scm:git:git@github.com:" + fallbackGitHubOrganization + "$3"));
        pattern = Pattern.compile("(.*github.com[:|/])([^/]*)(.*)");
        matcher = pattern.matcher(connectionURLPomData);
        matcher.find();
        connectionURLs.add(matcher.replaceFirst("$1" + fallbackGitHubOrganization + "$3"));
        return connectionURLs;
    }

    private boolean localCheckoutProvided() {
        File localCheckoutDir = config.getLocalCheckoutDir();
        return localCheckoutDir != null && localCheckoutDir.exists();
    }

    private boolean onlyOnePluginIncluded() {
        return config.getIncludePlugins().size() == 1;
    }

    /**
     * Scans through a WAR file, accumulating plugin information
     *
     * @param war WAR to scan
     * @param pluginRegExp The plugin regexp to use, can be used to differentiate between detached
     *     or "normal" plugins in the war file
     * @return Update center data
     */
    @SuppressFBWarnings(value = "REDOS", justification = "intended behavior")
    static UpdateSite.Data scanWAR(File war, String pluginRegExp) {
        UpdateSite.Entry core = null;
        List<UpdateSite.Plugin> plugins = new ArrayList<>();
        try (JarFile jf = new JarFile(war)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                Matcher m = Pattern.compile(JENKINS_CORE_FILE_REGEX).matcher(name);
                if (m.matches()) {
                    if (core != null) {
                        throw new IllegalStateException(">1 jenkins-core.jar in " + war);
                    }
                    // http://foobar is used to workaround the check in
                    // https://github.com/jenkinsci/jenkins/commit/f8daafd0327081186c06555f225e84c420261b4c
                    // We do not really care about the value
                    core = new UpdateSite.Entry("core", m.group(1), "https://foobar");
                }

                m = Pattern.compile(pluginRegExp).matcher(name);
                if (m.matches()) {
                    try (InputStream is = jf.getInputStream(entry);
                            JarInputStream jis = new JarInputStream(is)) {
                        Manifest manifest = jis.getManifest();
                        String shortName = manifest.getMainAttributes().getValue("Short-Name");
                        if (shortName == null) {
                            shortName = manifest.getMainAttributes().getValue("Extension-Name");
                            if (shortName == null) {
                                shortName = m.group(1);
                            }
                        }
                        String longName = manifest.getMainAttributes().getValue("Long-Name");
                        String version = manifest.getMainAttributes().getValue("Plugin-Version");
                        // Remove extra build information from the version number
                        final Matcher matcher =
                                Pattern.compile("^(.+-SNAPSHOT)(.+)$").matcher(version);
                        if (matcher.matches()) {
                            version = matcher.group(1);
                        }
                        String url = "jar:" + war.toURI() + "!/" + name;
                        UpdateSite.Plugin plugin =
                                new UpdateSite.Plugin(shortName, version, url, longName);
                        plugins.add(plugin);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (core == null) {
            throw new IllegalStateException("no jenkins-core.jar in " + war);
        }
        LOGGER.log(
                Level.INFO,
                "Scanned contents of {0} with {1} plugins",
                new Object[] {war, plugins.size()});
        return new UpdateSite.Data(core, plugins);
    }
}
