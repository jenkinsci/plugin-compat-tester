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
import org.jenkins.tools.test.model.PluginRemoting;

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
        
        for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
        	testPluginAgainst(coreVersion, pluginEntry.getValue().url);
        }
	}
	
	public void testPluginAgainst(String coreVersion, String hpiRemoteUrl){
		try {
			PluginRemoting remote = new PluginRemoting(hpiRemoteUrl);
			String pomContent = remote.retrievePomContent();
			
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
