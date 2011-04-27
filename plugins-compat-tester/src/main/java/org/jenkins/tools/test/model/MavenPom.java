package org.jenkins.tools.test.model;

import hudson.maven.MavenEmbedder;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenRequest;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MavenPom {

	private File rootDir;
	private String pomFileName;
    private File m2SettingsFile;
	
	public MavenPom(File rootDir, File m2SettingsFile){
		this(rootDir, "pom.xml", m2SettingsFile);
	}
	
	public MavenPom(File rootDir, String pomFileName, File m2SettingsFile){
		this.rootDir = rootDir;
		this.pomFileName = pomFileName;
        this.m2SettingsFile = m2SettingsFile;
	}
	
	public void transformPom(MavenCoordinates coreCoordinates) throws PomTransformationException{
		File pom = new File(rootDir.getAbsolutePath()+"/"+pomFileName);
		File backupedPom = new File(rootDir.getAbsolutePath()+"/"+pomFileName+".backup");
		try {
			FileUtils.rename(pom, backupedPom);
			
			Source xmlSource = new StreamSource(backupedPom);
			Source xsltSource = new StreamSource(new ClassPathResource("mavenParentReplacer.xsl").getInputStream());
			Result result = new StreamResult(pom);
			
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xsltSource);
			transformer.setParameter("parentArtifactId", coreCoordinates.artifactId);
			transformer.setParameter("parentGroupId", coreCoordinates.groupId);
			transformer.setParameter("parentVersion", coreCoordinates.version);
			transformer.transform(xmlSource, result);
		} catch (Exception e) {
			throw new PomTransformationException("Error while transforming pom : "+pom.getAbsolutePath(), e);
		}
		
	}

    public MavenExecutionResult executeGoals(MavenEmbedder mavenEmbedder, MavenRequest mavenRequest) throws PomExecutionException {

        final List<String> succeededPlugins = new ArrayList<String>();

        AbstractExecutionListener mavenListener = new AbstractExecutionListener(){
            public void mojoSucceeded(ExecutionEvent event){
                 succeededPlugins.add(event.getMojoExecution().getArtifactId());
            }
        };
        MavenExecutionResult result;
        try {

            result = mavenEmbedder.execute(mavenRequest);
        }catch(MavenEmbedderException e){
            // TODO: better manage this exception
            throw new RuntimeException("Error during maven embedder execution", e);
        }

        if(!result.getExceptions().isEmpty()){
            throw new PomExecutionException("Error while executing pom goals : "+ Arrays.toString(mavenRequest.getGoals().toArray()), result.getExceptions(), succeededPlugins);
        }

        return result;
    }
}
