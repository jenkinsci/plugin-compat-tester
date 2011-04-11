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
import org.jenkins.tools.test.model.MavenPom;
import org.jenkins.tools.test.model.PluginRemoting;

public class PluginCompatTester {

	private String updateCenterUrl;
	private String parentGroupId;
	private String parentArtifactId;
	private String parentVersion = null;
	private File workDirectory;
	
	public PluginCompatTester(String updateCenterUrl, String parentGAV, File workDirectory){
		this.updateCenterUrl = updateCenterUrl;
		String[] gavChunks = parentGAV.split(":");
		assert gavChunks.length == 3 || gavChunks.length == 2;
		this.parentGroupId = gavChunks[0];
		this.parentArtifactId = gavChunks[1];
		if(gavChunks.length == 3){
			this.parentVersion = gavChunks[2];
		}
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
    			// TODO: manage the exception in an Error databean, and jump to the next plugin !
    			throw e;
        	}        		
        }
	}
	
	public void testPluginAgainst(String coreVersion, String pluginName, String hpiRemoteUrl) throws Exception {
		File pluginCheckoutDir = new File(workDirectory.getAbsolutePath()+"/"+pluginName+"/");
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		PluginRemoting remote = new PluginRemoting(hpiRemoteUrl);
		String scmConnection = remote.retrieveScmConnection();
		
		ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
		ScmRepository repository = scmManager.makeScmRepository(scmConnection);
		CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(pluginCheckoutDir));
		
		if(!result.isSuccess()){
			throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
		}
		
		MavenPom pom = new MavenPom(pluginCheckoutDir);
        // If core version has not been set in GAV : use the latest available
        // in update center
		pom.transformPom(parentGroupId, parentArtifactId, parentVersion==null?coreVersion:parentVersion);
		
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
