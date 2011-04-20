package org.jenkins.tools.test.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.springframework.core.io.ClassPathResource;

public class MavenPom {

	private File rootDir;
	private String pomFileName;
	
	public MavenPom(File rootDir){
		this(rootDir, "pom.xml");
	}
	
	public MavenPom(File rootDir, String pomFileName){
		this.rootDir = rootDir;
		this.pomFileName = pomFileName;
	}
	
	public void transformPom(String newParentGroupId, String newParentArtifactId, String newParentVersion) throws PomTransformationException{
		File pom = new File(rootDir.getAbsolutePath()+"/"+pomFileName);
		File backupedPom = new File(rootDir.getAbsolutePath()+"/"+pomFileName+".backup");
		try {
			FileUtils.moveFile(pom, backupedPom);
			
			Source xmlSource = new StreamSource(backupedPom);
			Source xsltSource = new StreamSource(new ClassPathResource("mavenParentReplacer.xsl").getFile());
			Result result = new StreamResult(pom);
			
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xsltSource);
			transformer.setParameter("parentArtifactId", newParentArtifactId);
			transformer.setParameter("parentGroupId", newParentGroupId);
			transformer.setParameter("parentVersion", newParentVersion);
			transformer.transform(xmlSource, result);
		} catch (Exception e) {
			throw new PomTransformationException("Error while transforming pom : "+pom.getAbsolutePath(), e);
		}
		
	}

    public MavenExecutionResult executeGoals(List goals) throws PomExecutionException {
        final List<String> succeededPlugins = new ArrayList<String>();
        MavenRequest mavenRequest = new MavenRequest();
        //mavenRequest.setPom(pluginCheckoutDir.getAbsolutePath()+"/pom.xml");
        mavenRequest.setBaseDirectory(rootDir.getAbsolutePath());
        mavenRequest.setGoals(goals);
        AbstractExecutionListener mavenListener = new AbstractExecutionListener(){
            public void mojoSucceeded(ExecutionEvent event){
                 succeededPlugins.add(event.getMojoExecution().getArtifactId());
            }
        };
        mavenRequest.setExecutionListener(mavenListener);
        mavenRequest.getUserProperties().put( "failIfNoTests", "false" );
        mavenRequest.setPom(rootDir.getAbsolutePath()+"/pom.xml");

        MavenExecutionResult result;
        try {
            MavenEmbedder embedder = new MavenEmbedder(Thread.currentThread().getContextClassLoader(), mavenRequest);
            result = embedder.execute(mavenRequest);
        }catch(MavenEmbedderException e){
            // TODO: better manage this exception
            throw new RuntimeException("Error during maven embedder execution", e);
        } catch(ComponentLookupException e){
            // TODO: better manage this exception
            throw new RuntimeException("Error during maven embedder execution", e);
        }

        if(!result.getExceptions().isEmpty()){
            throw new PomExecutionException("Error while executing pom goals : "+ Arrays.toString(goals.toArray()), result.getExceptions(), succeededPlugins);
        }

        return result;
    }
}
