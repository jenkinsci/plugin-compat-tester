/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
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

import org.apache.tools.ant.filters.StringInputStream;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.xpath.XPath;

/**
 * Utility class providing business for retrieving plugin scm data
 * @author Frederic Camblor
 */
public class PluginRemoting {

	private String hpiRemoteUrl;
	
	public PluginRemoting(String hpiRemoteUrl){
		this.hpiRemoteUrl = hpiRemoteUrl;
	}
	
	private String retrievePomContent() throws PluginSourcesUnavailableException{
		try {
			URL pluginUrl = new URL(hpiRemoteUrl);
			ZipInputStream zin = new ZipInputStream(pluginUrl.openStream());
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
			
			String content = sb.toString();
			return content;
			
		}catch(Exception e){
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while retrieving pom content in hpi !", e);
		}
	}
	
	public PomData retrievePomData() throws PluginSourcesUnavailableException {
		String scmConnection = null;
        String artifactId = null;
		String pomContent = this.retrievePomContent();
        MavenCoordinates parent;
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
			Document doc = builder.parse(new StringInputStream(pomContent));
			
			XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
			XPathExpression scmConnectionXPath = xpath.compile("/project/scm/connection/text()");
            XPathExpression artifactIdXPath = xpath.compile("/project/artifactId/text()");

			scmConnection = (String)scmConnectionXPath.evaluate(doc, XPathConstants.STRING);
            artifactId = (String)artifactIdXPath.evaluate(doc, XPathConstants.STRING);

            parent = new MavenCoordinates(xpath.evaluate("/project/parent/groupId/text()", doc), xpath.evaluate("/project/parent/artifactId/text()", doc), xpath.evaluate("/project/parent/version/text()", doc));
		} catch (ParserConfigurationException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem during pom.xml parsing", e);
		} catch (SAXException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem during pom.xml parsing", e);
		} catch (IOException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem during pom.xml parsing", e);
		} catch (XPathExpressionException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while retrieving plugin's scm connection", e);
		}
		
		PomData pomData = new PomData(artifactId, scmConnection, parent);
        computeScmConnection(pomData);
        return pomData;
	}

    public static void computeScmConnection(PomData pomData) {
        String transformedConnectionUrl = pomData.getConnectionUrl();

        // Triming url
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
        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("(http(s)?://)?[^@:]+@github\\.com", "git://github.com");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl is using a github account instead of a read-only url git://github.com/...");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("://github\\.com[^/]", "://github.com/");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should have a '/' after the github.com url");
        }

        oldUrl = transformedConnectionUrl;
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("((ssh)|(http(s)?))://github\\.com", "git://github.com");
        if(!oldUrl.equals(transformedConnectionUrl)){
            pomData.getWarningMessages().add("project.scm.connectionUrl should be accessed in read-only mode (with git:// protocol)");
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
