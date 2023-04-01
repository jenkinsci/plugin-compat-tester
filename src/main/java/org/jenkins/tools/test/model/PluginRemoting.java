/*
 * The MIT License
 *
 * Copyright (c) 2004-2018, Sun Microsystems, Inc., Kohsuke Kawaguchi,
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

package org.jenkins.tools.test.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Utility class providing business for retrieving plugin POM data
 *
 * @author Frederic Camblor
 */
public class PluginRemoting {

    private String hpiRemoteUrl;
    private File pomFile;

    public PluginRemoting(String hpiRemoteUrl) {
        this.hpiRemoteUrl = hpiRemoteUrl;
    }

    public PluginRemoting(File pomFile) {
        this.pomFile = pomFile;
    }

    private String retrievePomContent() {
        try {
            if (hpiRemoteUrl != null) {
                return retrievePomContentFromHpi();
            } else {
                return retrievePomContentFromXmlFile();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressFBWarnings(value = "URLCONNECTION_SSRF_FD", justification = "Only file: URLs are supported")
    private String retrievePomContentFromHpi() throws IOException {
        URL url = new URL(hpiRemoteUrl);
        if (!url.getProtocol().equals("jar") || !url.getFile().startsWith("file:")) {
            throw new MalformedURLException("Invalid URL: " + url);
        }
        try (InputStream is = url.openStream();
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().startsWith("META-INF/maven/") && ze.getName().endsWith("/pom.xml")) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new FileNotFoundException("Failed to retrieve POM content from HPI: " + hpiRemoteUrl);
    }

    private String retrievePomContentFromXmlFile() throws IOException {
        return Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
    }

    public Model retrieveModel() {
        String pomContent = this.retrievePomContent();

        Model model;
        try (Reader r = new StringReader(pomContent)) {
            MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
            model = mavenXpp3Reader.read(r);
        } catch (XmlPullParserException e) {
            throw new IllegalArgumentException("Failed to parse pom.xml", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Scm scm = model.getScm();
        if (scm != null) {
            // scm may contain properties so it needs to be resolved.
            scm.setConnection(interpolateString(scm.getConnection(), model.getArtifactId()));
        }

        return model;
    }

    /**
     * Replaces any occurence of {@code "${project.artifactId}"} or {@code "${artifactId}"} with the
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
