package org.jenkins.tools.test.model;

import java.io.IOException;
import java.net.URL;
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

import org.apache.tools.ant.filters.StringInputStream;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PluginRemoting {

	private String hpiRemoteUrl;
	
	public PluginRemoting(String hpiRemoteUrl){
		this.hpiRemoteUrl = hpiRemoteUrl;
	}
	
	public String retrievePomContent() throws PluginSourcesUnavailableException{
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
	
	public String retrieveScmConnection() throws PluginSourcesUnavailableException {
		String result = null;
		String pomContent = this.retrievePomContent();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
			Document doc = builder.parse(new StringInputStream(pomContent));
			
			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			XPathExpression expr = xpath.compile("/project/scm/connection/text()");
			
			result = (String)expr.evaluate(doc, XPathConstants.STRING);
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
		
		return transformScmConnectionUrlDependingOnSillyRules(result);
	}

    private static String transformScmConnectionUrlDependingOnSillyRules(String connectionUrl){
        String transformedConnectionUrl = connectionUrl;
		// Just fixing some scm-sync-configuration issues...
		// TODO: remove this when fixed !
		if(transformedConnectionUrl.endsWith(".git") && !(transformedConnectionUrl.endsWith("-plugin.git"))){
			transformedConnectionUrl = transformedConnectionUrl.substring(0, transformedConnectionUrl.length()-4)+"-plugin.git";
        }

        // Java.net SVN migration
        transformedConnectionUrl = transformedConnectionUrl.replaceAll("svn.dev.java.net/svn/hudson/", "svn.java.net/svn/hudson~svn/");

        return transformedConnectionUrl;
    }
}
