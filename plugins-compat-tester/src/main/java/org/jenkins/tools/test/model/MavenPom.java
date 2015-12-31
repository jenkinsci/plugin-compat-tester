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
import org.jenkins.tools.test.exception.PomTransformationException;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
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

    public void transformPom(MavenCoordinates coreCoordinates, boolean testJenkinsVersion) throws PomTransformationException{
        File pom = new File(rootDir.getAbsolutePath()+"/"+pomFileName);
        File backupedPom = new File(rootDir.getAbsolutePath()+"/"+pomFileName+".backup");
        try {
            Document doc = new SAXReader().read(pom);
            FileUtils.rename(pom, backupedPom);
            
            Element parent = doc.getRootElement().element("parent");
            if (testJenkinsVersion) {
                doc = modJenkinsTestVersion(doc, parent);
            }
            
            parent.element("groupId").setText(coreCoordinates.groupId);
            parent.element("artifactId").setText(coreCoordinates.artifactId);
            parent.element("version").setText(coreCoordinates.version);
            
            FileWriter w = new FileWriter(pom);
            try {
                doc.write(w);
            } finally {
                w.close();
            }
        } catch (Exception e) {
            throw new PomTransformationException("Error while transforming pom : "+pom.getAbsolutePath(), e);
        }
    }
    
    private Document modJenkinsTestVersion(Document pom, Element parent){
        //in dependencies
        Element dependencies = pom.getRootElement().element("dependencies");
        if (dependencies == null) {
            dependencies = pom.getRootElement().addElement("dependencies");
        }
        
        Element testJenkins = dependencies.addElement("dependency");
        testJenkins.addElement("groupId").addText("org.jenkins-ci.main");
        testJenkins.addElement("artifactId").addText("jenkins-test-harness");
        testJenkins.addElement("version").addText(parent.element("version").getText());
        return pom;
    }

    public void addDependencies(Map<String,VersionNumber> toAdd, Map<String,VersionNumber> toReplace, VersionNumber coreDep, Map<String,String> pluginGroupIds) throws IOException {
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
            if (artifactId == null) {
                continue;
            }
            excludeSecurity144Compat(mavenDependency);
            VersionNumber replacement = toReplace.get(artifactId.getTextTrim());
            if (replacement == null) {
                continue;
            }
            Element version = mavenDependency.element("version");
            if (version != null) {
                mavenDependency.remove(version);
            }
            version = mavenDependency.addElement("version");
            version.addText(replacement.toString());
        }
        dependencies.addComment("SYNTHETIC");
        for (Map.Entry<String,VersionNumber> dep : toAdd.entrySet()) {
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
            excludeSecurity144Compat(dependency);
        }
        FileWriter w = new FileWriter(pom);
        try {
            doc.write(w);
        } finally {
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

}
