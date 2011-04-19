package org.jenkins.tools.test;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import hudson.model.UpdateSite;
import hudson.model.UpdateSite.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.cli.MavenLoggerManager;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.jenkins.tools.test.exception.PluginSourcesUnavailableException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
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
        testPlugins(null);
    }

	public void testPlugins(List<String> includedPluginNames) throws Exception {
        UpdateSite.Data data = extractUpdateCenterData();
        String coreVersion = data.core.version;
        
		SCMManagerFactory.getInstance().start();
        for(Entry<String, Plugin> pluginEntry : data.plugins.entrySet()){
            if(includedPluginNames==null || includedPluginNames.contains(pluginEntry.getValue().name)){
                try {
                    testPluginAgainst(coreVersion, pluginEntry.getValue());
                }catch(Exception e){
                    System.err.println("Error : " + e.getMessage());
                    // TODO: manage the exception in an Error databean, and jump to the next plugin !
                }
            }
        }
	}
	
	public void testPluginAgainst(String coreVersion, Plugin plugin) throws PluginSourcesUnavailableException, PomTransformationException {
		File pluginCheckoutDir = new File(workDirectory.getAbsolutePath()+"/"+plugin.name+"/");
		pluginCheckoutDir.mkdir();
		System.out.println("Created plugin checkout dir : "+pluginCheckoutDir.getAbsolutePath());
		
		PluginRemoting remote = new PluginRemoting(plugin.url);
		String scmConnection = remote.retrieveScmConnection();
		
		try {
			ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
			ScmRepository repository = scmManager.makeScmRepository(scmConnection);
			CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(pluginCheckoutDir), new ScmTag(plugin.name+"-"+plugin.version));
			
			if(!result.isSuccess()){
				throw new RuntimeException(result.getProviderMessage() + "||" + result.getCommandOutput());
			}
		} catch (ComponentLookupException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while creating ScmManager !", e);
		} catch (ScmException e) {
			System.err.println("Error : " + e.getMessage());
			throw new PluginSourcesUnavailableException("Problem while checkouting plugin sources !", e);
		}
		
		MavenPom pom = new MavenPom(pluginCheckoutDir);
        // If core version has not been set in GAV : use the latest available
        // in update center
		pom.transformPom(parentGroupId, parentArtifactId, parentVersion==null?coreVersion:parentVersion);
		
		// Calling maven
        try {
            MavenExecutionResult result = pom.executeGoals(Arrays.asList("test"));
        } catch (PomExecutionException e) {
            // TODO : manage succeededPlugins in exception !
        }
	}
	
	protected UpdateSite.Data extractUpdateCenterData(){
		URL url = null;
		String jsonp = null;
		try {
	        url = new URL(this.updateCenterUrl);
	        jsonp = IOUtils.toString(url.openStream());
		}catch(IOException e){
			throw new RuntimeException("Invalid update center url : "+this.updateCenterUrl, e);
		}
		
        String json = jsonp.substring(jsonp.indexOf('(')+1,jsonp.lastIndexOf(')'));

        UpdateSite us = new UpdateSite("default", url.toExternalForm());
        UpdateSite.Data data = null;
        try {
	        Constructor<UpdateSite.Data> dataConstructor = UpdateSite.Data.class.getDeclaredConstructor(UpdateSite.class, JSONObject.class);
	        dataConstructor.setAccessible(true);
	        data = dataConstructor.newInstance(us, JSONObject.fromObject(json));
        }catch(Exception e){
        	throw new RuntimeException("UpdateSite.Data instanciation problems", e);
        }
		
        return data;
	}
}
