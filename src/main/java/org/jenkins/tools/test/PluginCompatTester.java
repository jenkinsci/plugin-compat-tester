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
import hudson.util.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.WrappedPluginCompatibilityException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeCheckoutContext;
import org.jenkins.tools.test.model.hook.BeforeCompilationContext;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHooks;
import org.jenkins.tools.test.model.plugin_metadata.LocalCheckoutMetadataExtractor;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadata;
import org.jenkins.tools.test.model.plugin_metadata.PluginMetadataHooks;
import org.jenkins.tools.test.util.StreamGobbler;
import org.jenkins.tools.test.util.WarUtils;

/**
 * Frontend for plugin compatibility tests
 *
 * @author Frederic Camblor, Olivier Lamy
 */
@SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "intended behavior")
public class PluginCompatTester {

    private static final Logger LOGGER = Logger.getLogger(PluginCompatTester.class.getName());

    private final PluginCompatTesterConfig config;
    private final ExternalMavenRunner runner;

    public PluginCompatTester(PluginCompatTesterConfig config) {
        this.config = config;
        runner = new ExternalMavenRunner(config.getExternalMaven(), config.getMavenSettings(), config.getMavenArgs());
    }

    public void testPlugins() throws PluginCompatibilityTesterException {
        PluginCompatTesterHooks pcth =
                new PluginCompatTesterHooks(config.getExternalHooksJars(), config.getExcludeHooks());
        // Determine the plugin data

        String coreVersion = WarUtils.extractCoreVersionFromWar(config.getWar());
        List<PluginMetadata> pluginMetadataList = WarUtils.extractPluginMetadataFromWar(
                config.getWar(),
                PluginMetadataHooks.loadExtractors(config.getExternalHooksJars()),
                config.getIncludePlugins(),
                config.getExcludePlugins());

        // Filter any plugins that are not being tested, group by Git URL, and run through the before checkout hooks
        Map<String, List<PluginMetadata>> pluginsByrepo;
        try {
            pluginsByrepo = pluginMetadataList.stream()
                    .map(new RunAndMapBeforeCheckoutHooks(pcth, coreVersion, config))
                    .collect(Collectors.groupingBy(PluginMetadata::getGitUrl, LinkedHashMap::new, Collectors.toList()));
        } catch (WrappedPluginCompatibilityException e) {
            throw e.getCause();
        }

        if (localCheckoutProvided()) {
            // Do not perform the before checkout hooks on a local checkout
            List<PluginMetadata> localMetadata =
                    LocalCheckoutMetadataExtractor.extractMetadata(config.getLocalCheckoutDir(), config);
            pluginsByrepo.put(null, localMetadata);
        }

        LOGGER.log(Level.INFO, "Starting plugin tests on core version {0}", coreVersion);

        PluginCompatibilityTesterException lastException = null;

        for (Map.Entry<String, List<PluginMetadata>> entry : pluginsByrepo.entrySet()) {
            // Construct a single working directory for the clone
            String gitUrl = entry.getKey();

            File cloneDir;
            if (gitUrl == null) {
                cloneDir = config.getLocalCheckoutDir();
            } else {
                cloneDir = new File(config.getWorkingDir(), getRepoNameFromGitUrl(gitUrl));
                // All plugins from the same reactor are assumed to be of the same version
                String tag = entry.getValue().get(0).getGitHash();

                try {
                    cloneFromScm(gitUrl, config.getFallbackGitHubOrganization(), tag, cloneDir);
                } catch (PluginCompatibilityTesterException e) {
                    lastException = throwOrAddSuppressed(lastException, e, config.isFailFast());
                    LOGGER.log(
                            Level.SEVERE,
                            String.format("Internal error while cloning repository %s at commit %s.", gitUrl, tag),
                            e);
                    continue;
                }
            }
            // For each of the PluginMetadata entries, go test the plugin
            for (PluginMetadata pm : entry.getValue()) {
                try {
                    testPluginAgainst(coreVersion, pm, cloneDir, pcth);
                } catch (PluginCompatibilityTesterException e) {
                    lastException = throwOrAddSuppressed(lastException, e, config.isFailFast());
                    LOGGER.log(
                            Level.SEVERE,
                            String.format(
                                    "Internal error while executing a test for core %s and plugin %s at version %s.",
                                    coreVersion, pm.getName(), pm.getVersion()),
                            e);
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
    }

    private static File createBuildLogFile(File workDirectory, PluginMetadata metadata, String coreVersion) {

        File f = new File(workDirectory.getAbsolutePath()
                + File.separator
                + createBuildLogFilePathFor(metadata.getPluginId(), metadata.getVersion(), coreVersion));
        try {
            Files.createDirectories(f.getParentFile().toPath());
            Files.deleteIfExists(f.toPath());
            Files.createFile(f.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create build log file", e);
        }
        return f;
    }

    private static String createBuildLogFilePathFor(String pluginId, String pluginVersion, String coreVersion) {
        return String.format("logs/%s/v%s_against_core_version_%s.log", pluginId, pluginVersion, coreVersion);
    }

    private void testPluginAgainst(
            String coreVersion, PluginMetadata pluginMetadata, File cloneLocation, PluginCompatTesterHooks pcth)
            throws PluginCompatibilityTesterException {
        LOGGER.log(
                Level.INFO,
                "\n\n\n\n\n\n"
                        + "#############################################\n"
                        + "#############################################\n"
                        + "##\n"
                        + "## Starting to test {0} {1} against core version {2}\n"
                        + "##\n"
                        + "#############################################\n"
                        + "#############################################\n\n\n\n\n",
                new Object[] {pluginMetadata.getName(), pluginMetadata.getVersion(), coreVersion});

        File buildLogFile = createBuildLogFile(config.getWorkingDir(), pluginMetadata, coreVersion);

        // Run the before compile hooks
        BeforeCompilationContext beforeCompile =
                new BeforeCompilationContext(pluginMetadata, coreVersion, config, cloneLocation);
        pcth.runBeforeCompilation(beforeCompile);

        // First build against the original POM. This defends against source incompatibilities
        // (which we do not care about for this purpose); and ensures that we are testing a
        // plugin binary as close as possible to what was actually released. We also skip
        // potential javadoc execution to avoid general test failure.
        // as we may need to resolve artifacts of incrementals or CD releases ensure we
        // use the same version so parents and other dependencies can be found
        // if not an incremental this property is ignored so is safe to set.
        runner.run(
                Map.of(
                        "maven.javadoc.skip", "true",
                        "set.changelist", "true"),
                cloneLocation,
                pluginMetadata.getModule(),
                buildLogFile,
                "clean",
                "process-test-classes");

        List<String> args = new ArrayList<>();
        args.add("hpi:resolve-test-dependencies");
        args.add("hpi:test-hpl");
        args.add("surefire:test");

        // Run preexecution hooks
        BeforeExecutionContext forExecutionHooks =
                new BeforeExecutionContext(pluginMetadata, coreVersion, config, cloneLocation, args);
        pcth.runBeforeExecution(forExecutionHooks);

        Map<String, String> properties = new LinkedHashMap<>(config.getMavenProperties());
        properties.put("overrideWar", config.getWar().toString());
        properties.put("jenkins.version", coreVersion);
        properties.put("useUpperBounds", "true");
        // enable setting the incremental / CD release so if a multi-module reactor
        // any other projects can be obtained from the repo
        properties.put("set.changelist", "true");
        // as hooks may be adjusting the POMs tell the git-changelist extension to ignore dirty
        // commits
        properties.put("ignore.dirt", "true");
        if (new VersionNumber(coreVersion).isOlderThan(new VersionNumber("2.382"))) {
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
                cloneLocation,
                pluginMetadata.getModule(),
                buildLogFile,
                args.toArray(new String[0]));
    }

    public static void cloneFromScm(
            String url, String fallbackGitHubOrganization, String scmTag, File checkoutDirectory)
            throws PluginSourcesUnavailableException {
        List<String> gitUrls = new ArrayList<>();
        gitUrls.add(url);
        if (fallbackGitHubOrganization != null) {
            gitUrls = getFallbackGitUrl(gitUrls, url, fallbackGitHubOrganization);
        }

        PluginSourcesUnavailableException lastException = null;
        for (String gitUrl : gitUrls) {
            // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/

            // TODO pending release of
            // https://github.com/jenkinsci/antisamy-markup-formatter-plugin/pull/106
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/antisamy-markup-formatter-plugin",
                    "https://github.com/jenkinsci/antisamy-markup-formatter-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/authentication-tokens-plugin/pull/106
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/authentication-tokens-plugin",
                    "https://github.com/jenkinsci/authentication-tokens-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/aws-global-configuration-plugin/pull/51
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/aws-global-configuration-plugin",
                    "https://github.com/jenkinsci/aws-global-configuration-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/blueocean-display-url-plugin/pull/227
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/blueocean-display-url-plugin",
                    "https://github.com/jenkinsci/blueocean-display-url-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/bootstrap5-api-plugin/commit/8c5f60ab5e21c03b68d696e7b760caa991b25aa9
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/bootstrap5-api-plugin",
                    "https://github.com/jenkinsci/bootstrap5-api-plugin");

            // TODO pending backport of
            // https://github.com/jenkinsci/cloudbees-folder-plugin/pull/260
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/cloudbees-folder-plugin",
                    "https://github.com/jenkinsci/cloudbees-folder-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/configuration-as-code-plugin/pull/2166
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/configuration-as-code-plugin",
                    "https://github.com/jenkinsci/configuration-as-code-plugin");

            // TODO pending backport of
            // https://github.com/jenkinsci/custom-folder-icon-plugin/pull/109
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/custom-folder-icon-plugin",
                    "https://github.com/jenkinsci/custom-folder-icon-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/data-tables-api-plugin/commit/97dc7555017e6c7ea17f0b67cc292773f1114a54
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/data-tables-api-plugin",
                    "https://github.com/jenkinsci/data-tables-api-plugin");

            // TODO pending backport of
            // https://github.com/jenkinsci/echarts-api-plugin/commit/d6951a26e6f1c27b82c8308359f7f76e182de3e3
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/echarts-api-plugin", "https://github.com/jenkinsci/echarts-api-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/file-parameters-plugin/pull/142
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/file-parameters-plugin",
                    "https://github.com/jenkinsci/file-parameters-plugin");

            // TODO pending release of https://github.com/jenkinsci/github-api-plugin/pull/182
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/github-api-plugin", "https://github.com/jenkinsci/github-api-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/google-kubernetes-engine-plugin/pull/312
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/google-kubernetes-engine-plugin",
                    "https://github.com/jenkinsci/google-kubernetes-engine-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/google-metadata-plugin/pull/50
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/google-metadata-plugin",
                    "https://github.com/jenkinsci/google-metadata-plugin");

            // TODO pending release of https://github.com/jenkinsci/google-oauth-plugin/pull/176
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/google-oauth-plugin",
                    "https://github.com/jenkinsci/google-oauth-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/kubernetes-credentials-plugin/pull/37
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/kubernetes-credentials-plugin",
                    "https://github.com/jenkinsci/kubernetes-credentials-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/kubernetes-credentials-provider-plugin/pull/75
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/kubernetes-credentials-provider-plugin",
                    "https://github.com/jenkinsci/kubernetes-credentials-provider-plugin");

            // TODO pending adoption of https://github.com/jenkinsci/matrix-auth-plugin/pull/131
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/matrix-auth-plugin", "https://github.com/jenkinsci/matrix-auth-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/node-iterator-api-plugin/pull/11
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/node-iterator-api-plugin",
                    "https://github.com/jenkinsci/node-iterator-api-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/oauth-credentials-plugin/pull/9
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/oauth-credentials-plugin",
                    "https://github.com/jenkinsci/oauth-credentials-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/popper2-api-plugin/commit/bf781e31b072103f3f72d7195e9071863f7f4dd9
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/popper2-api-plugin", "https://github.com/jenkinsci/popper2-api-plugin");

            // TODO pending release of https://github.com/jenkinsci/pubsub-light-plugin/pull/100
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/pubsub-light-plugin",
                    "https://github.com/jenkinsci/pubsub-light-plugin");

            // TODO pending release of https://github.com/jenkinsci/s3-plugin/pull/243
            gitUrl = gitUrl.replace("git://github.com/jenkinsci/s3-plugin", "https://github.com/jenkinsci/s3-plugin");

            // TODO pending release of https://github.com/jenkinsci/ssh-agent-plugin/pull/116
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/ssh-agent-plugin", "https://github.com/jenkinsci/ssh-agent-plugin");

            // TODO pending release of https://github.com/jenkinsci/ssh-slaves-plugin/pull/352
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/ssh-slaves-plugin", "https://github.com/jenkinsci/ssh-slaves-plugin");

            // TODO pending release of
            // https://github.com/jenkinsci/theme-manager-plugin/pull/154
            gitUrl = gitUrl.replace(
                    "git://github.com/jenkinsci/theme-manager-plugin",
                    "https://github.com/jenkinsci/theme-manager-plugin");
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

        // git init
        Process p = new ProcessBuilder()
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

        p = new ProcessBuilder()
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
        p = new ProcessBuilder()
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
                        "git checkout FETCH_HEAD failed with exit status " + exitStatus + ": " + output);
            }
        } catch (InterruptedException e) {
            throw new PluginSourcesUnavailableException("git checkout FETCH_HEAD was interrupted", e);
        }
    }

    public static List<String> getFallbackGitUrl(
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

    public static String getGitUrlFromLocalCheckout(File workingDirectory, File localCheckout, MavenRunner runner)
            throws PluginSourcesUnavailableException, PomExecutionException {
        try {
            File log = new File(workingDirectory, "localcheckout-scm-connection.log");
            runner.run(
                    Map.of("expression", "project.scm.connection", "output", log.getAbsolutePath()),
                    localCheckout,
                    null,
                    null,
                    "-q",
                    "help:evaluate");
            List<String> output = Files.readAllLines(log.toPath(), Charset.defaultCharset());
            String scm = output.get(output.size() - 1);
            if (scm.startsWith("scm:git:")) {
                Files.deleteIfExists(log.toPath());
                return scm.substring(8);
            }
            throw new PluginSourcesUnavailableException(
                    "SCM " + scm + " is not a supported URL, only Git is supported by the PCT");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    static class RunAndMapBeforeCheckoutHooks implements Function<PluginMetadata, PluginMetadata> {

        private PluginCompatTesterHooks pcth;
        private String coreVersion;
        private PluginCompatTesterConfig config;

        RunAndMapBeforeCheckoutHooks(
                PluginCompatTesterHooks pcth, String coreVersion, PluginCompatTesterConfig config) {
            this.pcth = pcth;
            this.coreVersion = coreVersion;
            this.config = config;
        }

        @Override
        public PluginMetadata apply(PluginMetadata pluginMetadata) throws WrappedPluginCompatibilityException {
            BeforeCheckoutContext c = new BeforeCheckoutContext(pluginMetadata, coreVersion, config);
            try {
                pcth.runBeforeCheckout(c);
            } catch (PluginCompatibilityTesterException e) {
                throw new WrappedPluginCompatibilityException(e);
            }
            return c.getPluginMetadata();
        }
    }

    /**
     * Throws {@code current} if {@code throwException} is {@code true} or returns {@code caught}
     * with {@code current} added (if non-null) as a suppressed exception.
     *
     * @param <T>
     * @param current the PluginCompatibilityTesterException if any
     * @param caught the newly caught exception
     * @param throwException {@code true} if we should immediately rethrow {@code caught}, {@code
     *     false} indicating we should return {@caught}.
     * @return {@code caught}
     * @throws PluginCompatibilityTesterException if {@code throwException == true} then {@caught}
     *     is thrown.
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
}
