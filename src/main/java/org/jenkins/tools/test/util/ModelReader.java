package org.jenkins.tools.test.util;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkins.tools.test.exception.MetadataExtractionException;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;

/**
 * Utility methods to load a {@link Model}
 */
public class ModelReader {

    /**
     * Load the model that is embedded inside the plugin in {@code
     * META-INF/maven/${groupId}/${artifactId}/pom.xml}
     *
     * @param groupId the groupId of the plugin
     * @param artifactId the artifactId of the plugin
     * @param jarInputStream the input stream created from the plugin's JAR file.
     * @return the Maven model for the plugin as read from the {@code META-INF} directory.
     * @throws PluginSourcesUnavailableException if the entry could not be loaded or found.
     * @throws IOException if there was an I/O related issue obtaining the model.
     */
    public static Model getPluginModelFromHpi(String groupId, String artifactId, JarInputStream jarInputStream)
            throws MetadataExtractionException, IOException {
        Model model = null;
        final String entryName = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml";
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (entryName.equals(jarEntry.getName())) {
                try {
                    MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
                    model = mavenXpp3Reader.read(jarInputStream);
                    break;
                } catch (XmlPullParserException e) {
                    throw new MetadataExtractionException("Failed to parse pom.xml", e);
                }
            }
        }
        if (model == null) {
            throw new MetadataExtractionException(entryName + " was not found in the plugin HPI");
        }

        Scm scm = model.getScm();
        if (scm != null) {
            // scm may contain properties, so it needs to be resolved.
            scm.setConnection(interpolateString(scm.getConnection(), model.getArtifactId()));
        }
        return model;
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
