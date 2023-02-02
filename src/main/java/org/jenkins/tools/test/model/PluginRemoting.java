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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility class providing business for retrieving plugin POM data
 *
 * @author Frederic Camblor
 */
public class PluginRemoting {

    private static final Logger LOGGER = Logger.getLogger(PluginRemoting.class.getName());
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

    private String retrievePomContentFromHpi() throws IOException {
        URL url = new URL(hpiRemoteUrl);
        try (InputStream is = url.openStream();
                ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().startsWith("META-INF/maven/")
                        && ze.getName().endsWith("/pom.xml")) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new FileNotFoundException("Failed to retrieve POM content from HPI: " + hpiRemoteUrl);
    }

    private String retrievePomContentFromXmlFile() throws IOException {
        return Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
    }

    public PomData retrievePomData() throws PluginSourcesUnavailableException {
        String scmConnection;
        String scmTag;
        String artifactId;
        String groupId;
        String packaging;
        String pomContent = this.retrievePomContent();
        @CheckForNull MavenCoordinates parent = null;

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(pomContent)));

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            XPathExpression scmConnectionXPath = xpath.compile("/project/scm/connection/text()");
            XPathExpression artifactIdXPath = xpath.compile("/project/artifactId/text()");
            XPathExpression groupIdXPath = xpath.compile("/project/groupId/text()");
            XPathExpression packagingXPath = xpath.compile("/project/packaging/text()");
            XPathExpression scmTagXPath = xpath.compile("/project/scm/tag/text()");

            scmConnection = (String) scmConnectionXPath.evaluate(doc, XPathConstants.STRING);
            scmTag =
                    StringUtils.trimToNull(
                            (String) scmTagXPath.evaluate(doc, XPathConstants.STRING));
            artifactId = (String) artifactIdXPath.evaluate(doc, XPathConstants.STRING);
            groupId = (String) groupIdXPath.evaluate(doc, XPathConstants.STRING);
            packaging =
                    StringUtils.trimToNull(
                            (String) packagingXPath.evaluate(doc, XPathConstants.STRING));

            String parentNode = xpath.evaluate("/project/parent", doc);
            if (parentNode != null && !parentNode.isBlank()) {
                LOGGER.log(Level.FINE, "Parent POM: {0}", parentNode);
                parent =
                        new MavenCoordinates(
                                getValueOrFail(doc, xpath, "/project/parent/groupId"),
                                getValueOrFail(doc, xpath, "/project/parent/artifactId"),
                                getValueOrFail(doc, xpath, "/project/parent/version"));
            } else {
                LOGGER.log(Level.FINE, "No parent POM for {0}", artifactId);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.log(Level.WARNING, "Failed to parse pom.xml", e);
            throw new PluginSourcesUnavailableException("Failed to parse pom.xml", e);
        } catch (XPathExpressionException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve SCM connection", e);
            throw new PluginSourcesUnavailableException("Failed to retrieve SCM connection", e);
        }

        PomData pomData =
                new PomData(artifactId, packaging, scmConnection, scmTag, parent, groupId);
        computeScmConnection(pomData);
        return pomData;
    }

    /**
     * Retrieves a field value by XPath. The value must exist and be non-empty.
     *
     * @throws IOException parsing error
     */
    @NonNull
    private static String getValueOrFail(Document doc, XPath xpath, String field)
            throws IOException {
        String res;
        try {
            res = xpath.evaluate(field + "/text()", doc);
        } catch (XPathExpressionException e) {
            throw new IOException("Expression failed for the field " + field, e);
        }

        if (res == null || res.isBlank()) {
            throw new IOException("Field is either null or blank: " + field);
        }
        return res;
    }

    public static void computeScmConnection(PomData pomData) {
        String transformedConnectionUrl = pomData.getConnectionUrl();

        // Trimming url
        transformedConnectionUrl = transformedConnectionUrl.trim();

        // Generally, when connectionUrl is empty, is implies it is declared in a parent pom
        // => Only possibility is to deduct github repository from artifactId (crossing fingers it
        // is not a bizarre repository url...)
        String oldUrl = transformedConnectionUrl;
        if (transformedConnectionUrl.isEmpty()) {
            transformedConnectionUrl =
                    "scm:git:git://github.com/jenkinsci/"
                            + pomData.artifactId.replaceAll("jenkins", "")
                            + "-plugin.git";
            if (!oldUrl.equals(transformedConnectionUrl)) {
                LOGGER.log(
                        Level.WARNING,
                        "project.scm.connectionUrl is not present in plugin's pom .. isn't it"
                                + " residing somewhere on a parent pom ?");
            }
        }

        // Java.net SVN migration
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl =
                transformedConnectionUrl.replaceAll(
                        "(svn|hudson)\\.dev\\.java\\.net/svn/hudson/",
                        "svn.java.net/svn/hudson~svn/");
        if (!oldUrl.equals(transformedConnectionUrl)) {
            LOGGER.log(
                    Level.WARNING,
                    "project.scm.connectionUrl is pointing to svn.dev.java.net/svn/hudson/ instead"
                            + " of svn.java.net/svn/hudson~svn/");
        }

        // ${project.artifactId}
        transformedConnectionUrl =
                transformedConnectionUrl.replaceAll(
                        "\\$\\{project\\.artifactId\\}", pomData.artifactId);

        // github url like [https://]<username>@github.com/...
        // => Replaced by git://github.com/...
        /* Do not change this; it was actually correct if the repo was non-public.
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("(http(s)?://)?[^@:]+@github\\.com", "git://github.com");
        if(!oldUrl.equals(transformedConnectionUrl)){
            LOGGER.log(Level.WARNING, "project.scm.connectionUrl is using a github account instead of a read-only url git://github.com/...");
        }
        */

        // Convert things like
        // scm:git:git://git@github.com:jenkinsci/dockerhub-notification-plugin.git
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl =
                transformedConnectionUrl.replaceAll(
                        "scm:git:git://git@github\\.com:jenkinsci",
                        "scm:git:git://github.com/jenkinsci");
        if (!oldUrl.equals(transformedConnectionUrl)) {
            LOGGER.log(
                    Level.WARNING,
                    "project.scm.connectionUrl should should be accessed in read-only mode (with"
                            + " git:// protocol)");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl =
                transformedConnectionUrl.replaceAll("://github\\.com[^/]", "://github.com/");
        if (!oldUrl.equals(transformedConnectionUrl)) {
            LOGGER.log(
                    Level.WARNING,
                    "project.scm.connectionUrl should have a '/' after the github.com url");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl =
                transformedConnectionUrl.replaceAll(
                        "://github\\.com/hudson/", "://github.com/jenkinsci/");
        if (!oldUrl.equals(transformedConnectionUrl)) {
            LOGGER.log(
                    Level.WARNING,
                    "project.scm.connectionUrl should not reference hudson project anymore (no"
                            + " plugin repository there))");
        }

        // Just fixing some scm-sync-configuration issues...
        // TODO: remove this when fixed !
        oldUrl = transformedConnectionUrl;
        if ("scm-sync-configuration".equals(pomData.artifactId)) {
            transformedConnectionUrl =
                    transformedConnectionUrl.substring(0, transformedConnectionUrl.length() - 4)
                            + "-plugin.git";
        }
        if (!oldUrl.equals(transformedConnectionUrl)) {
            LOGGER.log(
                    Level.WARNING, "project.scm.connectionUrl should be ending with '-plugin.git'");
        }

        pomData.setConnectionUrl(transformedConnectionUrl);
    }
}
