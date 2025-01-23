package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExpressionEvaluator;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class JenkinsTestHarnessHook extends PluginCompatTesterHookBeforeExecution {
    public static final String VERSION_WITH_WEB_FRAGMENTS = "2386.v82359624ea_05";
    public static final List<String> VALID_VERSIONS =
            List.of("2244.2247.ve6b_a_8191b_95f", "2270.2272.vd890c8c611b_3", VERSION_WITH_WEB_FRAGMENTS);
    private static final String PROPERTY_NAME = "jenkins-test-harness.version";

    private static boolean usesWebFragment(@NonNull BeforeExecutionContext context) {
        // We only want this hook to be enabled if the Jetty 12 hook is not enabled at the same time
        var war = context.getConfig().getWar();
        try (var jarFile = new JarFile(war)) {
            var jenkinsCoreEntry = getJenkinsCoreEntry(jarFile);
            if (jenkinsCoreEntry == null) {
                throw new IllegalArgumentException("Failed to find jenkins-core jar in " + war);
            }
            try (var is = jarFile.getInputStream(jenkinsCoreEntry);
                    var bis = new BufferedInputStream(is);
                    var jis = new JarInputStream(bis)) {
                return hasWebFragment(jis);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to determine whether " + war + " uses web fragments", e);
        }
    }

    @NonNull
    private static VersionNumber getPropertyAsVersion(
            @NonNull BeforeExecutionContext context, @NonNull String propertyName) throws PomExecutionException {
        var expressionEvaluator = new ExpressionEvaluator(
                context.getCloneDirectory(),
                context.getPlugin().getModule(),
                new ExternalMavenRunner(context.getConfig()));
        return new VersionNumber(expressionEvaluator.evaluateString(propertyName));
    }

    private static boolean hasWebFragment(JarInputStream jis) throws IOException {
        for (var entry = jis.getNextEntry(); entry != null; entry = jis.getNextEntry()) {
            if ("META-INF/web-fragment.xml".equals(entry.getName())) {
                return true;
            }
        }
        return false;
    }

    private static JarEntry getJenkinsCoreEntry(JarFile jarFile) {
        for (var entries = jarFile.entries(); entries.hasMoreElements(); ) {
            var entry = entries.nextElement();
            if (entry.getName().startsWith("WEB-INF/lib/jenkins-core-")) {
                return entry;
            }
        }
        return null;
    }

    private static VersionNumber getWinstoneVersion(File war) {
        try (var jarFile = new JarFile(war)) {
            var zipEntry = jarFile.getEntry("executable/winstone.jar");
            if (zipEntry == null) {
                throw new IllegalArgumentException("Failed to find winstone.jar in " + war);
            }
            try (var is = jarFile.getInputStream(zipEntry);
                    var bis = new BufferedInputStream(is);
                    var jis = new JarInputStream(bis)) {
                var manifest = jis.getManifest();
                if (manifest == null) {
                    throw new IllegalArgumentException("Failed to read manifest in " + war);
                }
                var version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (version == null) {
                    throw new IllegalArgumentException("Failed to read Winstone version from manifest in " + war);
                }
                return new VersionNumber(version);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Winstone version in " + war, e);
        }
    }

    /**
     * Determines the version of Jenkins Test Harness to use depending on the original version.
     */
    @NonNull
    static VersionNumber determineNextVersion(@NonNull VersionNumber version) {
        var first = true;
        VersionNumber older = null;
        for (var validVersion : VALID_VERSIONS.stream().map(VersionNumber::new).collect(Collectors.toList())) {
            if (first) {
                if (validVersion.isNewerThan(version)) {
                    return validVersion;
                }
                first = false;
            }
            if (validVersion.isOlderThan(version)) {
                older = validVersion;
            } else {
                if (older != null) {
                    return validVersion;
                }
            }
        }
        // Keep the version as is. If that happens, it means check() method returned true by mistake.
        return version;
    }

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        var winstoneVersion = getWinstoneVersion(context.getConfig().getWar());
        if (winstoneVersion.getDigitAt(0) < 7) {
            // Don't upgrade anything if winstone version is too old.
            return false;
        }
        try {
            var existingVersion = getPropertyAsVersion(context, PROPERTY_NAME);
            // If core uses web fragments, we need a version of jth with web fragments support
            if (!usesWebFragment(context)) {
                return existingVersion.isOlderThan(new VersionNumber(VERSION_WITH_WEB_FRAGMENTS));
            } else {
                return VALID_VERSIONS.stream().map(VersionNumber::new).anyMatch(existingVersion::equals)
                        || existingVersion.isOlderThan(new VersionNumber(VERSION_WITH_WEB_FRAGMENTS));
            }
        } catch (PomExecutionException e) {
            return false;
        }
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) throws PluginCompatibilityTesterException {
        var version = getPropertyAsVersion(context, PROPERTY_NAME);
        context.getArgs().add(String.format("-D%s=%s", PROPERTY_NAME, determineNextVersion(version)));
        /*
         * The version of JUnit 5 used at runtime must match the version of JUnit 5 used to compile the tests, but the
         * inclusion of a newer test harness might cause the HPI plugin to try to use a newer version of JUnit 5 at
         * runtime to satisfy upper bounds checks, so exclude JUnit 5 from upper bounds analysis.
         */
        context.getUpperBoundsExcludes().add("org.junit.jupiter:junit-jupiter-api");
    }
}
