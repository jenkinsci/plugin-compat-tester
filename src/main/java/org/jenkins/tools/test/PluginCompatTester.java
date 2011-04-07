package org.jenkins.tools.test;

import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.model.PluginRemoting;

public class PluginCompatTester {

	private String updateCenterUrl;
	private String parentGAV;
	private File workDirectory;
	
	public PluginCompatTester(String updateCenterUrl, String parentGAV, File workDirectory){
		this.updateCenterUrl = updateCenterUrl;
		this.parentGAV = parentGAV;
		this.workDirectory = workDirectory;
	}
	
	public void testPlugins() throws Exception {
        UpdateSite.Data data = extractUpdateCenterData();
        String coreVersion = data.core.version;
        
		SCMManagerFactory.getInstance().start();
        for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
        	try {
        		testPluginAgainst(coreVersion, pluginEntry.getValue().name, pluginEntry.getValue().url);
        	}catch(Exception e){
    			System.err.println("Error : " + e.getMessage());
    			throw e;
        	}        		
        }
	}
	
	public void testPluginAgainst(String coreVersion, String pluginName, String hpiRemoteUrl) throws Exception {
		PluginRemoting remote = new PluginRemoting(hpiRemoteUrl);
		String scmConnection = remote.retrieveScmConnection();
		
		File pluginCheckoutDir = new File(workDirectory.getAbsolutePath()+"/"+pluginName+"/");
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
		ScmRepository repository = scmManager.makeScmRepository(scmConnection);
		CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(pluginCheckoutDir));
		
		if(!result.isSuccess()){
			throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
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
