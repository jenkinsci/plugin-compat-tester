/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.tools.test.model;

import hudson.util.VersionNumber;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.io.XMLWriter;
import org.jenkins.tools.test.exception.PomTransformationException;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;

/**
 * Class encapsulating business around maven poms
 * @author Frederic Camblor
 */
public class MavenPom {

	private File rootDir;
	private String pomFileName;
	
	public MavenPom(File rootDir){
		this(rootDir, "pom.xml");
	}
	
	private MavenPom(File rootDir, String pomFileName){
		this.rootDir = rootDir;
		this.pomFileName = pomFileName;
	}

	public void transformPom(MavenCoordinates coreCoordinates) throws PomTransformationException{
		File pom = new File(rootDir.getAbsolutePath()+"/"+pomFileName);
		File backupedPom = new File(rootDir.getAbsolutePath()+"/"+pomFileName+".backup");
		try {
			FileUtils.rename(pom, backupedPom);

			Source xmlSource = new StreamSource(backupedPom);
            // TODO switch to DOM4J for simplicity and consistency
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

    public void addDependencies(Map<String,VersionNumber> toAdd, Map<String,VersionNumber> toReplace, Map<String,VersionNumber> toAddTest, Map<String,VersionNumber> toReplaceTest, VersionNumber coreDep, Map<String,String> pluginGroupIds, List<String> toConvert) 
        throws IOException 
    {
        File pom = new File(rootDir.getAbsolutePath() + "/" + pomFileName);
        Document doc;
        try {
            doc = new SAXReader().read(pom);
        } catch (DocumentException x) {
            throw new IOException(x);
        }
        Element dependencies = doc.getRootElement().element("dependencies");
        if (dependencies == null) {
            dependencies = doc.getRootElement().addElement("dependencies");
        }
        for (Element mavenDependency : (List<Element>) dependencies.elements("dependency")) {
            Element artifactId = mavenDependency.element("artifactId");
            if (artifactId == null || !"maven-plugin".equals(artifactId.getTextTrim())) {
                continue;
            }
            Element version = mavenDependency.element("version");
            if (version == null || version.getTextTrim().startsWith("${")) {
                // Prior to 1.532, plugins sometimes assumed they could pick up the Maven plugin version from their parent POM.
                if (version != null) {
                    mavenDependency.remove(version);
                }
                version = mavenDependency.addElement("version");
                version.addText(coreDep.toString());
            }
        }
        for (Element mavenDependency : (List<Element>) dependencies.elements("dependency")) {
            Element artifactId = mavenDependency.element("artifactId");
            Element groupId = mavenDependency.element("groupId");
            if (artifactId == null || groupId == null) {
                continue;
            }

            String expectedGroupId = pluginGroupIds.get(artifactId.getTextTrim());
            if(expectedGroupId == null || !groupId.getTextTrim().equals(expectedGroupId)) {
                continue;
            }
            
            excludeSecurity144Compat(mavenDependency);
            VersionNumber replacement = toReplace.get(artifactId.getTextTrim());
            if (replacement == null) {
                replacement = toReplaceTest.get(artifactId.getTextTrim());
                if (replacement == null) {
                    continue;
                }
                toReplaceTest.remove(artifactId.getTextTrim());
            }
            Element version = mavenDependency.element("version");
            if (version != null) {
                mavenDependency.remove(version);
            }
            version = mavenDependency.addElement("version");
            if (toConvert.contains(artifactId)) { // Remove the test scope
                Element scope = mavenDependency.element("scope");
                if (scope != null) {
                    mavenDependency.remove(scope);
                }
            }
            version.addText(replacement.toString());
            toReplace.remove(artifactId.getTextTrim());
        }
        // If the replacement dependencies weren't explicitly present in the pom, add them directly now
        toAdd.putAll(toReplace);
        toAddTest.putAll(toReplaceTest);

        dependencies.addComment("SYNTHETIC");
        addPlugins(toAdd, pluginGroupIds, dependencies, null);
        addPlugins(toAddTest, pluginGroupIds, dependencies, "test");

        FileWriter w = new FileWriter(pom);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(w, format);
        try {
            writer.write(doc);
        } finally {
            writer.close();
            w.close();
        }
    }

    /** JENKINS-25625 workaround. */
    private void excludeSecurity144Compat(Element dependency) {
        Element exclusions = dependency.element("exclusions");
        if (exclusions == null) {
            exclusions = dependency.addElement("exclusions");
        }
        Element exclusion = exclusions.addElement("exclusion");
        exclusion.addElement("groupId").addText("org.jenkins-ci");
        exclusion.addElement("artifactId").addText("SECURITY-144-compat");
    }

    /**
     * Add the given new plugins to the pom file. 
     */
    private void addPlugins(Map<String,VersionNumber> adding, Map<String,String> pluginGroupIds, Element dependencies, String scope) {
        for (Map.Entry<String,VersionNumber> dep : adding.entrySet()) {
            Element dependency = dependencies.addElement("dependency");
            String group = pluginGroupIds.get(dep.getKey());

            // Handle cases where plugin isn't under default groupId
            if (group != null && !group.isEmpty()) {
                dependency.addElement("groupId").addText(group);
            } else {
                dependency.addElement("groupId").addText("org.jenkins-ci.plugins");
            }
            dependency.addElement("artifactId").addText(dep.getKey());
            dependency.addElement("version").addText(dep.getValue().toString());

            // Add required scope
            if(scope != null) {
                dependency.addElement("scope").addText(scope);
            }
            excludeSecurity144Compat(dependency);
        }
    }

}
