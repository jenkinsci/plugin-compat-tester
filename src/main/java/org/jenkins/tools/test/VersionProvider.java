package org.jenkins.tools.test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] {getPCTVersionString(), getJavaVersionString()};
    }

    @SuppressFBWarnings(
            value = {"NP_LOAD_OF_KNOWN_NULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE"},
            justification =
                    "spotbugs false poisitive due to try-with-resources / https://github.com/spotbugs/spotbugs/issues/2191")
    private static String getPCTVersionString() {
        try (InputStream manifestStream =
                VersionProvider.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (manifestStream != null) {
                Manifest mf = new Manifest(manifestStream);
                System.out.println(mf);
                Attributes mainAttributes = mf.getMainAttributes();
                return "PCT : "
                        + mainAttributes.getValue("Implementation-Version")
                        + " "
                        + mainAttributes.getValue("scmRevision");
            }
        } catch (IOException e) {
            // ignored
        }
        return "PCT : SNAPSHOT build";
    }

    private static String getJavaVersionString() {
        return "Java : " + System.getProperty("java.version");
    }
}
