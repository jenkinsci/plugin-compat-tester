package org.jenkins.tools.test.model;

import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
}
