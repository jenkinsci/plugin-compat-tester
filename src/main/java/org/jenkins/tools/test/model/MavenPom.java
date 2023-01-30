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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.util.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jenkins.tools.test.exception.PomTransformationException;

/**
 * Class encapsulating business around Maven POMs
 *
 * @author Frederic Camblor
 */
public class MavenPom {

    private static final Logger LOGGER = Logger.getLogger(MavenPom.class.getName());

    private final static String GROUP_ID_ELEMENT = "groupId";
    private final static String ARTIFACT_ID_ELEMENT = "artifactId";
    private final static String VERSION_ELEMENT = "version";
    private final static String CLASSIFIER_ELEMENT = "classifier";

    private File rootDir;
    private String pomFileName;

    public MavenPom(File rootDir) {
        this(rootDir, "pom.xml");
    }

    private MavenPom(File rootDir, String pomFileName) {
        this.rootDir = rootDir;
        this.pomFileName = pomFileName;
    }

    public void transformPom(MavenCoordinates coreCoordinates) throws PomTransformationException {
        File pom = new File(rootDir.getAbsolutePath() + "/" + pomFileName);
        File backupPom = new File(rootDir.getAbsolutePath() + "/" + pomFileName + ".backup");
        try {
            Files.move(pom.toPath(), backupPom.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Document doc;
            try {
                doc = new SAXReader().read(backupPom);
            } catch (DocumentException x) {
                throw new IOException(x);
            }

            Element parent = doc.getRootElement().element("parent");
            if (parent != null) {
                Element groupIdElem = parent.element(GROUP_ID_ELEMENT);
                if (groupIdElem != null) {
                    groupIdElem.setText(coreCoordinates.groupId);
                }

                Element artifactIdElem = parent.element(ARTIFACT_ID_ELEMENT);
                if (artifactIdElem != null) {
                    artifactIdElem.setText(coreCoordinates.artifactId);
                }

                Element versionIdElem = parent.element(VERSION_ELEMENT);
                if (versionIdElem != null) {
                    versionIdElem.setText(coreCoordinates.version);
                }
            }

            writeDocument(pom, doc);
        } catch (Exception e) {
            throw new PomTransformationException("Error while transforming pom : " + pom.getAbsolutePath(), e);
        }
    }

    /**
     * Removes the dependency if it exists.
     */
    public void removeDependency(@NonNull String groupId, @NonNull String artifactId) throws IOException {
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

        for (Element mavenDependency : dependencies.elements("dependency")) {
            Element artifactIdElem = mavenDependency.element(ARTIFACT_ID_ELEMENT);
            if (artifactIdElem == null || !artifactId.equalsIgnoreCase(artifactIdElem.getText())) {
                continue;
            }

            Element groupIdElem = mavenDependency.element(GROUP_ID_ELEMENT);
            if (groupIdElem != null && groupId.equalsIgnoreCase(groupIdElem.getText())) {
                LOGGER.log(Level.WARNING, "Removing dependency on {0}:{1}",
                        new Object[]{groupId, artifactId});
                dependencies.remove(mavenDependency);
            }
        }

        writeDocument(pom, doc);
    }
    
    /**
     * Create/Update a plugin management section with a set of plugins 
     * @param includeGroupId - specify if we want to add the groupId or not 
     */
    public void addPluginManagement(List<MavenCoordinates> pluginsToAdd, boolean includeGroupId) throws IOException {
        File pom = new File(rootDir.getAbsolutePath() + "/" + pomFileName);
        Document doc;
        try {
            doc = new SAXReader().read(pom);
        } catch (DocumentException x) {
            throw new IOException(x);
        }
        
        Element build = doc.getRootElement().element("build");
        if (build == null) {
            build = doc.getRootElement().addElement("build");
        }
        
        Element pluginManagement = build.element("pluginManagement");
        if (pluginManagement == null) {
            pluginManagement = build.addElement("pluginManagement");
        }
        
        Element plugins = pluginManagement.element("plugins");
        if (plugins == null) {        
            plugins = pluginManagement.addElement("plugins");
        }
        for (MavenCoordinates plugin : pluginsToAdd) {
            Element entry = plugins.addElement("plugin");
            if (includeGroupId) {
                Element groupIdElem = entry.addElement(GROUP_ID_ELEMENT);
                groupIdElem.setText(plugin.groupId);
            }
            Element artifactIdElem = entry.addElement(ARTIFACT_ID_ELEMENT);
            artifactIdElem.setText(plugin.artifactId);
            Element versionIdElem = entry.addElement(VERSION_ELEMENT);
            versionIdElem.setText(plugin.version);
        }
        
        writeDocument(pom, doc);
    }
    
    /**
     * Create/Update the properties section adding/updating some of them
     */
    public void addProperties(Properties propertiesToAdd) throws IOException {
        File pom = new File(rootDir.getAbsolutePath() + "/" + pomFileName);
        Document doc;
        try {
            doc = new SAXReader().read(pom);
        } catch (DocumentException x) {
            throw new IOException(x);
        }
        
        Element properties = doc.getRootElement().element("properties");
        if (properties == null) {
            properties = doc.getRootElement().addElement("properties");
        }
        
        for (Entry<Object, Object> property : propertiesToAdd.entrySet()) {
            String key = (String)property.getKey();
            Element entry = properties.element(key);
            if (entry == null) {
                entry = properties.addElement(key);
            }
            entry.setText((String)property.getValue());
        }
        writeDocument(pom, doc);
    }

    public void addDependencies(Map<String, VersionNumber> toAdd, Map<String, VersionNumber> toReplace, Map<String, VersionNumber> toAddTest, Map<String, VersionNumber> toReplaceTest, Map<String, String> pluginGroupIds, List<String> toConvert)
            throws IOException {
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

        manageDependencies(toAdd, toReplace, toAddTest, toReplaceTest, pluginGroupIds, doc, dependencies, true);
        Element profiles = doc.getRootElement().element("profiles");
        if (profiles != null) {
            Map<String, VersionNumber> empty = new HashMap<>();
            Iterator<Element> elementIterator = profiles.elementIterator("profile");
            while(elementIterator.hasNext()) {
                Element e = elementIterator.next();
                Element profileDependencies = e.element("dependencies");
                if (profileDependencies == null) {
                    continue;
                }
                manageDependencies(empty, toReplace, empty, toReplaceTest, pluginGroupIds, doc, profileDependencies, false);
            }
        }
        writeDocument(pom, doc);
    }

    private void manageDependencies(Map<String, VersionNumber> toAdd, Map<String, VersionNumber> toReplace,
            Map<String, VersionNumber> toAddTest, Map<String, VersionNumber> toReplaceTest,
            Map<String, String> pluginGroupIds, Document doc, Element dependencies, boolean addition) {
        Set<String> depsWithoutClassifier = new HashSet<>();
        for (Element mavenDependency : dependencies.elements("dependency")) {
            Element artifactId = mavenDependency.element(ARTIFACT_ID_ELEMENT);
            if (mavenDependency.element(CLASSIFIER_ELEMENT) == null) {
                depsWithoutClassifier.add(artifactId.getTextTrim());
            }
        }

        Element properties = doc.getRootElement().element("properties");
        Map<String, VersionNumber> toReplaceUsed = new LinkedHashMap<>();
        Map<String, VersionNumber> toReplaceTestUsed = new LinkedHashMap<>();
        for (Element mavenDependency : dependencies.elements("dependency")) {
            Element artifactId = mavenDependency.element(ARTIFACT_ID_ELEMENT);
            Element groupId = mavenDependency.element(GROUP_ID_ELEMENT);
            if (artifactId == null || groupId == null) {
                continue;
            }

            String expectedGroupId = pluginGroupIds.get(artifactId.getTextTrim());
            if (expectedGroupId == null || !groupId.getTextTrim().equals(expectedGroupId)) {
                continue;
            }

            String trimmedArtifactId = artifactId.getTextTrim();
            VersionNumber replacement = toReplace.get(trimmedArtifactId);
            if (replacement == null) {
                replacement = toReplaceTest.get(trimmedArtifactId);
                if (replacement == null) {
                    continue;
                }
                toReplaceTestUsed.put(trimmedArtifactId, replacement);
            }
            Element version = mavenDependency.element(VERSION_ELEMENT);
            if (version != null) {
                if (version.getTextTrim().startsWith("${")) {
                    // Search property and update its value
                    String property = version.getTextTrim().replace("${", "").replace("}", "");
                    Element propertyToUpdate = null;
                    for (Element mavenProperty : properties.elements()) {
                        if (StringUtils.equals(property, mavenProperty.getQName().getName())) {
                            propertyToUpdate = mavenProperty;
                            break;
                        }
                    }
                    if (propertyToUpdate != null) {
                        properties.remove(propertyToUpdate);
                        propertyToUpdate.setText(replacement.toString());
                        properties.add(propertyToUpdate);
                        toReplaceUsed.put(trimmedArtifactId, replacement);
                        continue;
                    }
                } else {
                    mavenDependency.remove(version);
                }
            }
            version = mavenDependency.addElement(VERSION_ELEMENT);
            version.addText(replacement.toString());
            Element scope = mavenDependency.element("scope");
            if (scope != null && scope.getTextTrim().equals("test")) {
                toReplaceTestUsed.put(trimmedArtifactId, replacement);
                if (toReplaceTest.containsKey(trimmedArtifactId) && !depsWithoutClassifier.contains(trimmedArtifactId)) { // https://github.com/jenkinsci/bom/pull/301#issuecomment-694518923
                    Element mainDep = mavenDependency.createCopy();
                    Element classifier = mainDep.element(CLASSIFIER_ELEMENT);
                    if (classifier != null) {
                        mainDep.remove(classifier);
                        dependencies.add(mainDep); // would prefer to insert just before mavenDependency but API does not seem to support this
                    }
                }
            } else {
                toReplaceUsed.put(trimmedArtifactId, replacement);
            }
        }
        
        if (addition) {
            // If the replacement dependencies weren't explicitly present in the pom, add them directly now
            toReplace.entrySet().removeAll(toReplaceUsed.entrySet());
            toReplaceTest.entrySet().removeAll(toReplaceTestUsed.entrySet());
            toAdd.putAll(toReplace);
            toAddTest.putAll(toReplaceTest);
    
            dependencies.addComment("SYNTHETIC");
            addPlugins(toAdd, pluginGroupIds, dependencies, null);
            addPlugins(toAddTest, pluginGroupIds, dependencies, "test");
        }
    }

    /**
     * Add the given new plugins to the pom file.
     */
    private void addPlugins(Map<String, VersionNumber> adding, Map<String, String> pluginGroupIds, Element dependencies, String scope) {
        for (Map.Entry<String, VersionNumber> dep : adding.entrySet()) {
            Element dependency = dependencies.addElement("dependency");
            String group = pluginGroupIds.get(dep.getKey());

            // Handle cases where plugin isn't under default groupId
            if (group != null && !group.isEmpty()) {
                dependency.addElement(GROUP_ID_ELEMENT).addText(group);
            } else {
                LOGGER.log(Level.WARNING, "No known group ID for plugin {0}", dep.getKey());
                dependency.addElement(GROUP_ID_ELEMENT).addText("org.jenkins-ci.plugins");
            }
            dependency.addElement(ARTIFACT_ID_ELEMENT).addText(dep.getKey());
            dependency.addElement(VERSION_ELEMENT).addText(dep.getValue().toString());

            // Add required scope
            if (scope != null) {
                dependency.addElement("scope").addText(scope);
            }
        }
    }

    private void writeDocument(final File target, final Document doc) throws IOException {
        Writer w = Files.newBufferedWriter(target.toPath(), getSafeCharset(doc));
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(w, format);
        try {
            writer.write(doc);
        } finally {
            writer.close();
            w.close();
        }
    }

    private Charset getSafeCharset(final Document doc) {
        try {
            return Charset.forName(doc.getXMLEncoding());
        } catch (Exception ex) {
            return Charset.defaultCharset();
        }
    }

}
