package org.jenkins.tools.test.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;

/**
 * Utility methods to load a {@link Model}
 */
public class ModelReader {

    /**
     * Load the model that is embedded inside the plugin in {@code META-INF/maven/${groupId}/${artifactId}/pom.xml}
     * @param groupId the groupId of the plugin
     * @param artifactId the artifactId of the plugin
     * @param jarInputStream the input stream created from the plugin's JAR file.
     * @return the Maven model for the plugin as read from the {@code META-INF} directory.
     * @throws PluginSourcesUnavailableException if the entry could not be loaded or found.
     * @throws IOException if there was an I/O related issue obtaining the model.
     */
    public static Model getPluginModelFromHpi(String groupId, String artifactId, JarInputStream jarInputStream)
            throws PluginSourcesUnavailableException, IOException {
        String pom = getPomFromHpi(groupId, artifactId, jarInputStream);
        Model model;

        try (Reader r = new StringReader(pom)) {
            MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
            model = mavenXpp3Reader.read(r);
        } catch (XmlPullParserException e) {
            throw new PluginSourcesUnavailableException("Failed to parse pom.xml", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Scm scm = model.getScm();
        if (scm != null) {
            // scm may contain properties, so it needs to be resolved.
            scm.setConnection(interpolateString(scm.getConnection(), model.getArtifactId()));
        }
        return model;
    }

    private static String getPomFromHpi(String groupId, String artifactId, JarInputStream jarInputStream)
            throws PluginSourcesUnavailableException, IOException {
        final String entryName = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml";
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (entryName.equals(jarEntry.getName())) {
                return new String(jarInputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new PluginSourcesUnavailableException(entryName + " was not found in the plugin HPI");
    }

    /**
     * Replace any occurrence of {@code "${project.artifactId}"} or {@code "${artifactId}"} with the
     * supplied value of the artifactId/
     *
     * @param original the original string
     * @param artifactId the interpolated String
     * @return the original string with any interpolation for the artifactId resolved.
     */
    static String interpolateString(String original, String artifactId) {
        return original.replace("${project.artifactId}", artifactId);
    }
}
