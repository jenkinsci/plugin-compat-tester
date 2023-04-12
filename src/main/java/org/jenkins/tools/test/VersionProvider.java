package org.jenkins.tools.test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    private static final String UNKNOWN_VERSION = "999999-SNAPSHOT";

    @Override
    public String[] getVersion() {
        return new String[] {getPctVersionString(), getJavaVersionString()};
    }

    @SuppressFBWarnings(
            value = {"NP_LOAD_OF_KNOWN_NULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE"},
            justification =
                    "SpotBugs false positive due to try-with-resources / https://github.com/spotbugs/spotbugs/issues/2191")
    private static String getPctVersionString() {
        StringBuilder sb = new StringBuilder("Plugin Compatibility Tester ");
        try (InputStream manifestStream = VersionProvider.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (manifestStream != null) {
                Manifest manifest = new Manifest(manifestStream);
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes.containsKey(new Attributes.Name("Implementation-Version"))) {
                    sb.append(mainAttributes.getValue("Implementation-Version"));
                } else {
                    sb.append(UNKNOWN_VERSION);
                }
                if (mainAttributes.containsKey(new Attributes.Name("Implementation-Build"))) {
                    sb.append(" (");
                    sb.append(mainAttributes.getValue("Implementation-Build"));
                    if (mainAttributes.containsKey(new Attributes.Name("Implementation-Build-Is-Tainted"))) {
                        String buildIsTainted = mainAttributes.getValue("Implementation-Build-Is-Tainted");
                        if (!"ok".equals(buildIsTainted)) {
                            sb.append('-');
                            sb.append(buildIsTainted);
                        }
                    }
                    sb.append(')');
                }
            } else {
                sb.append(UNKNOWN_VERSION);
            }
        } catch (IOException e) {
            sb.append(UNKNOWN_VERSION);
        }
        return sb.toString();
    }

    private static String getJavaVersionString() {
        return "Java version: "
                + System.getProperty("java.version", "<unknown Java version>")
                + ", vendor: "
                + System.getProperty("java.vendor", "<unknown vendor>")
                + ", runtime: "
                + System.getProperty("java.home", "<unknown runtime>");
    }
}
