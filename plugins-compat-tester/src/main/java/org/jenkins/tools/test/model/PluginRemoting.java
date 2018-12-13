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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.xpath.XPath;

/**
 * Utility class providing business for retrieving plugin POM data
 * @author Frederic Camblor
 */
public class PluginRemoting {

    private static final Logger LOGGER = Logger.getLogger(PluginRemoting.class.getName());
    private String hpiRemoteUrl;
    private File pomFile;

    public PluginRemoting(String hpiRemoteUrl){
        this.hpiRemoteUrl = hpiRemoteUrl;
    }

    public PluginRemoting(File pomFile){
        this.pomFile = pomFile;
    }
	
    private String retrievePomContent() throws PluginSourcesUnavailableException{
        if (hpiRemoteUrl != null) {
            return retrievePomContentFromHpi();
        } else {
            return retrievePomContentFromXmlFile();
        }
    }

    private String retrievePomContentFromHpi() throws PluginSourcesUnavailableException {
        try (InputStream pluginUrlStream = new URL(hpiRemoteUrl).openStream(); ZipInputStream zin = new ZipInputStream(pluginUrlStream)) {
            ZipEntry zipEntry = zin.getNextEntry();
            while(!zipEntry.getName().startsWith("META-INF/maven") || !zipEntry.getName().endsWith("pom.xml")){
                zin.closeEntry();
                zipEntry = zin.getNextEntry();
            }

            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int n;
            while ((n = zin.read(buf, 0, 1024)) > -1)
                sb.append(new String(buf, 0, n));

            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
            throw new PluginSourcesUnavailableException("Problem while retrieving pom content in hpi !", e);
        }
    }

    private String retrievePomContentFromXmlFile() throws PluginSourcesUnavailableException{
        try {
            return FileUtils.readFileToString(pomFile);
        } catch(Exception e) {
            System.err.println("Error : " + e.getMessage());
            throw new PluginSourcesUnavailableException(String.format("Problem while retrieving pom content from file %s", pomFile), e);
        }
    }
	
	public PomData retrievePomData() throws PluginSourcesUnavailableException {
		String scmConnection = null;
        String artifactId = null;
        String packaging;
		String pomContent = this.retrievePomContent();
        @CheckForNull MavenCoordinates parent = null;
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
			Document doc = builder.parse(new StringInputStream(pomContent));
			
			XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
			XPathExpression scmConnectionXPath = xpath.compile("/project/scm/connection/text()");
            XPathExpression artifactIdXPath = xpath.compile("/project/artifactId/text()");
            XPathExpression packagingXPath = xpath.compile("/project/packaging/text()");

			scmConnection = (String)scmConnectionXPath.evaluate(doc, XPathConstants.STRING);
            artifactId = (String)artifactIdXPath.evaluate(doc, XPathConstants.STRING);
            packaging = StringUtils.trimToNull((String)packagingXPath.evaluate(doc, XPathConstants.STRING));

            String parentNode = xpath.evaluate("/project/parent", doc);
            if (StringUtils.isNotBlank(parentNode)) {
                LOGGER.log(Level.SEVERE, parentNode.toString());
                parent = new MavenCoordinates(
                        getValueOrFail(doc, xpath, "/project/parent/groupId"),
                        getValueOrFail(doc, xpath, "/project/parent/artifactId"),
                        getValueOrFail(doc, xpath, "/project/parent/version"));
            } else {
                LOGGER.log(Level.WARNING, "No parent POM reference for artifact {0}, " +
                                "likely a plugin with Incrementals support is used (Jenkins JEP-305). " +
                                "Will try to ignore it (FTR https://issues.jenkins-ci.org/browse/JENKINS-55169). " +
                                "hpiRemoteUrl={1}, pomFile={2}",
                        new Object[] {artifactId, hpiRemoteUrl, pomFile});
            }
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem during pom.xml parsing", e);
		} catch (XPathExpressionException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while retrieving plugin's scm connection", e);
		}
		
		PomData pomData = new PomData(artifactId, packaging, scmConnection, parent);
        computeScmConnection(pomData);
        return pomData;
	}

    /**
     * Retrieves a field value by XPath.
     * The value must exist and be non-empty
     * @throws IOException parsing error
     */
	@Nonnull
	private static String getValueOrFail(Document doc, XPath xpath, String field) throws IOException {
        String res;
	    try {
	        res = xpath.evaluate(field + "/text()", doc);
        } catch (XPathExpressionException e) {
            throw new IOException("Expression failed for the field " + field, e);
        }

        if (StringUtils.isBlank(res)) {
            throw new IOException("Field is either null or blank: " + field);
        }
        return res;
    }

    public static void computeScmConnection(PomData pomData) {
        String transformedConnectionUrl = pomData.getConnectionUrl();

        // Trimming url
        transformedConnectionUrl = transformedConnectionUrl.trim();

        // Generally, when connectionUrl is empty, is implies it is declared in a parent pom
        // => Only possibility is to deduct github repository from artifactId (crossing fingers it is not
        // a bizarre repository url...)
        String oldUrl = transformedConnectionUrl;
        if(transformedConnectionUrl.isEmpty()){
            transformedConnectionUrl = "scm:git:git://github.com/jenkinsci/"+pomData.artifactId.replaceAll("jenkins", "")+"-plugin.git";
            if(!oldUrl.equals(transformedConnectionUrl)){
                pomData.getWarningMessages().add("project.scm.connectionUrl is not present in plugin's pom .. isn't it residing somewhere on a parent pom ?");
            }
        }

        // Java.net SVN migration
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("(svn|hudson)\\.dev\\.java\\.net/svn/hudson/", "svn.java.net/svn/hudson~svn/");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl is pointing to svn.dev.java.net/svn/hudson/ instead of svn.java.net/svn/hudson~svn/");
        }

        // ${project.artifactId}
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("\\$\\{project\\.artifactId\\}", pomData.artifactId);

        // github url like [https://]<username>@github.com/...
        // => Replaced by git://github.com/...
        /* Do not change this; it was actually correct if the repo was non-public.
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("(http(s)?://)?[^@:]+@github\\.com", "git://github.com");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl is using a github account instead of a read-only url git://github.com/...");
        }
        */

        //Convert things like scm:git:git://git@github.com:jenkinsci/dockerhub-notification-plugin.git
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("scm:git:git://git@github\\.com:jenkinsci", "scm:git:git://github.com/jenkinsci");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should should be accessed in read-only mode (with git:// protocol)");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("://github\\.com[^/]", "://github.com/");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should have a '/' after the github.com url");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("://github\\.com/hudson/", "://github.com/jenkinsci/");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should not reference hudson project anymore (no plugin repository there))");
        }

		// Just fixing some scm-sync-configuration issues...
		// TODO: remove this when fixed !
        oldUrl = transformedConnectionUrl;
		if("scm-sync-configuration".equals(pomData.artifactId)){
			transformedConnectionUrl = transformedConnectionUrl.substring(0, transformedConnectionUrl.length()-4)+"-plugin.git";
        }
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should be ending with '-plugin.git'");
        }

        pomData.setConnectionUrl(transformedConnectionUrl);
    }
}
