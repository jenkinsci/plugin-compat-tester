package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import org.jenkins.tools.test.exception.PluginCompatibilityTesterException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.maven.ExternalMavenRunner;
import org.jenkins.tools.test.maven.MavenRunner;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Old versions of the plugin parent POM hard-code EE 8. If such a version is in use but the core
 * uses EE 9 or later, work around the issue by overriding the version.
 *
 * @see <a href="https://github.com/jenkinsci/plugin-pom/issues/936">jenkinsci/plugin-pom#936</a>
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class ServletApiWorkaround extends PluginCompatTesterHookBeforeExecution {

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        if (JenkinsTestHarnessHook2.isEnabled()) {
            return false;
        }
        PluginCompatTesterConfig config = context.getConfig();
        MavenRunner runner =
                new ExternalMavenRunner(config.getExternalMaven(), config.getMavenSettings(), config.getMavenArgs());
        VersionNumber jakartaServletApiVersion = getJakartaServletApiVersion(
                context.getCloneDirectory(), context.getPlugin().getModule(), runner);
        if (jakartaServletApiVersion.isOlderThan(new VersionNumber("5"))) {
            VersionNumber enterpriseEditionVersion = getEnterpriseEditionVersion(config.getWar());
            if (enterpriseEditionVersion != null && enterpriseEditionVersion.isNewerThan(new VersionNumber("8"))) {
                return true;
            }
        }
        return false;
    }

    private VersionNumber getJakartaServletApiVersion(File pluginPath, String module, MavenRunner runner) {
        Path log = pluginPath.toPath().resolve("jakarta.servlet-api.log");
        try {
            runner.run(
                    Map.of(
                            "includeGroupIds",
                            "jakarta.servlet",
                            "includeArtifactIds",
                            "jakarta.servlet-api",
                            "includeScope",
                            "provided",
                            "outputFile",
                            log.toAbsolutePath().toString(),
                            "set.changelist",
                            "true",
                            "ignore.dirt",
                            "true"),
                    pluginPath,
                    module,
                    null,
                    "-q",
                    "dependency:collect");
        } catch (PomExecutionException e) {
            throw new RuntimeException(e);
        }
        List<String> output;
        try {
            output = Files.readAllLines(log, Charset.defaultCharset());
            Files.deleteIfExists(log);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String line : output) {
            if (!line.trim().startsWith("jakarta.servlet:jakarta.servlet-api:")) {
                continue;
            }
            return new VersionNumber(line.trim().split(":")[3]);
        }
        throw new RuntimeException("Failed to determine jakarta.servlet-api version");
    }

    private VersionNumber getEnterpriseEditionVersion(File war) {
        try (JarFile jarFile = new JarFile(war)) {
            ZipEntry zipEntry = jarFile.getEntry("executable/winstone.jar");
            if (zipEntry == null) {
                throw new IllegalArgumentException("Failed to find winstone.jar in " + war);
            }
            try (InputStream is = jarFile.getInputStream(zipEntry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    JarInputStream jis = new JarInputStream(bis)) {
                JarEntry jarEntry;
                while ((jarEntry = jis.getNextJarEntry()) != null) {
                    if (!jarEntry.isDirectory()) {
                        continue;
                    }
                    if ("META-INF/maven/org.eclipse.jetty.ee11/jetty-ee11-servlet/".equals(jarEntry.getName())) {
                        return new VersionNumber("11");
                    } else if ("META-INF/maven/org.eclipse.jetty.ee10/jetty-ee10-servlet/".equals(jarEntry.getName())) {
                        return new VersionNumber("10");
                    } else if ("META-INF/maven/org.eclipse.jetty.ee9/jetty-ee9-servlet/".equals(jarEntry.getName())) {
                        return new VersionNumber("9");
                    } else if ("META-INF/maven/org.eclipse.jetty.ee8/jetty-ee8-servlet/".equals(jarEntry.getName())) {
                        return new VersionNumber("8");
                    } else if ("META-INF/maven/org.eclipse.jetty/jetty-servlet/".equals(jarEntry.getName())) {
                        return new VersionNumber("8");
                    }
                }
                return null; // unknown
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Java EE version in " + war, e);
        }
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) throws PluginCompatibilityTesterException {
        context.getOverrideVersions().put("jakarta.servlet:jakarta.servlet-api", "5.0.0");
    }
}
