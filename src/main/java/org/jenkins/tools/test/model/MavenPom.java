package org.jenkins.tools.test.model;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
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
}
