package org.jenkins.tools.test;

import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;

public class PluginCompatTester {

	private String updateCenterUrl;
	private String parentGAV;
	
	public PluginCompatTester(String updateCenterUrl, String parentGAV){
		this.updateCenterUrl = updateCenterUrl;
		this.parentGAV = parentGAV;
	}
	
	public void testPlugins(){
        UpdateSite.Data data = extractUpdateCenterData();
        String coreVersion = data.core.version;
        System.out.println(coreVersion);
        
        for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
        	testPluginAgainst(coreVersion, pluginEntry.getValue().url);
        }
	}
	
	public void testPluginAgainst(String coreVersion, String pluginUrlStr){
		try {
			URL pluginUrl = new URL(pluginUrlStr);
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
			System.out.println(content);
			
		}catch(Exception e){
			System.err.println("Error : " + e.getMessage());
		}
	}
	
	protected UpdateSite.Data extractUpdateCenterData(){
		URL url = null;
		String jsonp = null;
		try {
	        url = new URL(this.updateCenterUrl);
	        jsonp = IOUtils.toString(url.openStream());
		}catch(MalformedURLException e){
			throw new RuntimeException(e);
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		
        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));

        UpdateSite us = new UpdateSite("default", url.toExternalForm());
        UpdateSite.Data data = null;
        try {
	        Constructor<UpdateSite.Data> dataConstructor = UpdateSite.Data.class.getDeclaredConstructor(UpdateSite.class, JSONObject.class);
	        dataConstructor.setAccessible(true);
	        data = dataConstructor.newInstance(us, JSONObject.fromObject(json));
        }catch(Exception e){
        	throw new RuntimeException(e);
        }
		
        return data;
	}
}
