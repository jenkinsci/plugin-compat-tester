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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.MXParserDomDriver;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * POJO allowing to store the Plugin Compatibility Tester report
 *
 * @author Frederic Camblor
 */
public class PluginCompatReport {
    private Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests;
    private SortedSet<MavenCoordinates> testedCoreCoordinates;
    private String testJavaVersion;

    public PluginCompatReport(){
        this.pluginCompatTests = new TreeMap<>();
        this.testedCoreCoordinates = new TreeSet<>();
    }

    public void add(PluginInfos infos, PluginCompatResult result){
        if(!this.pluginCompatTests.containsKey(infos)){
            this.pluginCompatTests.put(infos, new ArrayList<>());
        }

        List<PluginCompatResult> results = pluginCompatTests.get(infos);
        // Deleting existing result if it exists
        results.remove(result);
        results.add(result);

        // Updating maven testedMavenCoordinates
        this.testedCoreCoordinates.add(result.coreCoordinates);
    }

    public void save(File reportPath) throws IOException {
        // Ensuring every PluginCompatResult list is sorted
        for(List<PluginCompatResult> results : this.pluginCompatTests.values()){
            Collections.sort(results);
        }

        // Writing to a temporary report file ...
        File tempReportPath = new File(reportPath.getAbsolutePath() + ".tmp");
        try (Writer out =
                Files.newBufferedWriter(tempReportPath.toPath(), Charset.defaultCharset())) {
            out.write(String.format("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>%n"));
            out.write(String.format("<?xml-stylesheet href=\"" + getXslFilename(reportPath) + "\" type=\"text/xsl\"?>%n"));
            XStream xstream = createXStream();
            xstream.toXML(this, out);
            out.flush();
        }

        // When everything went well, let's overwrite old report XML file with the new one
        Files.move(tempReportPath.toPath(), reportPath.toPath(), StandardCopyOption.ATOMIC_MOVE);
    }

    public static String getXslFilename(@NonNull File reportPath){
        return getBaseFilename(reportPath)+".xsl";
    }

    public static File getXslFilepath(@NonNull File reportPath){
        return new File(getBaseFilepath(reportPath)+".xsl");
    }

    public static File getHtmlFilepath(@NonNull File reportPath){
        return new File(getBaseFilepath(reportPath)+".html");
    }

    public static String getBaseFilepath(@NonNull File reportPath){
        File parentFile = reportPath.getParentFile();
        if (parentFile == null) {
            throw new IllegalArgumentException("The report path " + reportPath + " does not have a directory specification. " +
                    "A correct path should be something like 'out/pct-report.xml'");
        }
        return parentFile.getAbsolutePath()+"/"+getBaseFilename(reportPath);
    }

    public static String getBaseFilename(File reportPath){
        return reportPath.getName().split("\\.")[0];
    }

    /**
     * Reads a compatibility report
     *
     * @param reportPath Report file path
     * @return Report. If the file does not exist, an empty report will be returned
     * @throws IOException Unexpected read error.
     */
    @NonNull
    public static PluginCompatReport fromXml(File reportPath) throws IOException {
        PluginCompatReport report;

        // Reading report file from reportPath
        XStream xstream = createXStream();
        try(FileInputStream istream = new FileInputStream(reportPath)) {
            report = (PluginCompatReport)xstream.fromXML(istream);
        } catch (FileNotFoundException e) {
            // Path doesn't exist => create a new report object
            report = new PluginCompatReport();
        }

        // Ensuring we are using a TreeMap for pluginCompatTests
        if(!(report.pluginCompatTests instanceof SortedMap)){
            report.pluginCompatTests = new TreeMap<>(report.pluginCompatTests);
        }

        return report;
    }

    private static XStream createXStream(){
        XStream xstream = new XStream(new MXParserDomDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.allowTypes(new Class[] {MavenCoordinates.class, PluginCompatReport.class, PluginCompatResult.class, PluginInfos.class});
        xstream.alias("pluginInfos", PluginInfos.class);
        xstream.alias("coord", MavenCoordinates.class);
        xstream.alias("compatResult", PluginCompatResult.class);
        xstream.alias("status", TestStatus.class);
        xstream.alias("report", PluginCompatReport.class);
        return xstream;
    }

    public SortedSet<MavenCoordinates> getTestedCoreCoordinates() {
        return new TreeSet<>(testedCoreCoordinates);
    }

    public void setTestJavaVersion(String testJavaVersion) {
        this.testJavaVersion = testJavaVersion;
    }

    public String getTestJavaVersion() {
        return testJavaVersion;
    }

    public Map<PluginInfos, List<PluginCompatResult>> getPluginCompatTests(){
        return new TreeMap<>(pluginCompatTests);
    }
}
