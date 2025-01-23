package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

/**
 * Ensure that if the core is on Jetty 12, that the test harness is on Jetty 12 as well.
 */
@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class Jetty12Hook extends PropertyVersionHook {

    @Override
    public String getProperty() {
        return "jenkins-test-harness.version";
    }

    @Override
    public String getMinimumVersion() {
        return "2244.v77428109fd1f";
    }

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        VersionNumber winstoneVersion = getWinstoneVersion(context.getConfig().getWar());
        if (winstoneVersion.getDigitAt(0) < 7) {
            return false;
        }
        return super.check(context);
    }

    private VersionNumber getWinstoneVersion(File war) {
        try (JarFile jarFile = new JarFile(war)) {
            ZipEntry zipEntry = jarFile.getEntry("executable/winstone.jar");
            if (zipEntry == null) {
                throw new IllegalArgumentException("Failed to find winstone.jar in " + war);
            }
            try (InputStream is = jarFile.getInputStream(zipEntry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    JarInputStream jis = new JarInputStream(bis)) {
                Manifest manifest = jis.getManifest();
                if (manifest == null) {
                    throw new IllegalArgumentException("Failed to read manifest in " + war);
                }
                String version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (version == null) {
                    throw new IllegalArgumentException("Failed to read Winstone version from manifest in " + war);
                }
                return new VersionNumber(version);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Winstone version in " + war, e);
        }
    }

    @Override
    public void action(@NonNull BeforeExecutionContext context) {
        super.action(context);
        /*
         * The version of JUnit 5 used at runtime must match the version of JUnit 5 used to compile the tests, but the
         * inclusion of a newer test harness might cause the HPI plugin to try to use a newer version of JUnit 5 at
         * runtime to satisfy upper bounds checks, so exclude JUnit 5 from upper bounds analysis.
         */
        context.getUpperBoundsExcludes().add("org.junit.jupiter:junit-jupiter-api");
    }
}
