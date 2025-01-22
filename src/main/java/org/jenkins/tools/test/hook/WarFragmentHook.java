package org.jenkins.tools.test.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import org.jenkins.tools.test.model.hook.BeforeExecutionContext;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeExecution;
import org.kohsuke.MetaInfServices;

@MetaInfServices(PluginCompatTesterHookBeforeExecution.class)
public class WarFragmentHook extends PropertyVersionHook {
    @Override
    public String getProperty() {
        return "jenkins-test-harness.version";
    }

    @Override
    public String getMinimumVersion() {
        return "2385.vfe86233d0d36";
    }

    @Override
    public boolean check(@NonNull BeforeExecutionContext context) {
        if (Jetty12Hook.staticCheck(context, getProperty(), Jetty12Hook.JTH_VERSION)) {
            return false;
        }
        // We only want this hook to be enabled if the Jetty 12 hook is not enabled at the same time
        File war = context.getConfig().getWar();
        try (JarFile jarFile = new JarFile(war)) {
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

    private static boolean hasWebFragment(JarInputStream jis) throws IOException {
        for (var entry = jis.getNextEntry(); entry != null; entry = jis.getNextEntry()) {
            if ("META-INF/web-fragment.xml".equals(entry.getName())) {
                return true;
            }
        }
        return false;
    }

    private JarEntry getJenkinsCoreEntry(JarFile jarFile) {
        for (var entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith("WEB-INF/lib/jenkins-core-")) {
                return entry;
            }
        }
        return null;
    }

}
