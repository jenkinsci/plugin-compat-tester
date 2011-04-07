package org.jenkins.tools.test.model;

import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.filters.StringInputStream;
import org.w3c.dom.Document;

public class PluginRemoting {

	private String hpiRemoteUrl;
	
	public PluginRemoting(String hpiRemoteUrl){
		this.hpiRemoteUrl = hpiRemoteUrl;
	}
	
	public String retrievePomContent() throws Exception {
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
			throw e;
		}
	}
	
	public String retrieveScmConnection() throws Exception {
		String pomContent = this.retrievePomContent();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		//docBuilderFactory.setNamespaceAware(true);
		//docBuilderFactory.setValidating(true);
		DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
		Document doc = builder.parse(new StringInputStream(pomContent));
		
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		XPathExpression expr = xpath.compile("/project/scm/connection/text()");
		
		String result = (String)expr.evaluate(doc, XPathConstants.STRING);
		
		// Just fixing some scm-sync-configuration issues...
		// TODO: remove this when fixed !
		if(result.endsWith(".git") && !(result.endsWith("-plugin.git"))){
			result = result.substring(0, result.length()-4)+"-plugin.git";
		}
		return result;
	}
}
