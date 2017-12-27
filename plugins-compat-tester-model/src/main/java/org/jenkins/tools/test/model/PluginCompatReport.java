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
import com.thoughtworks.xstream.io.xml.Xpp3DomDriver;
import hudson.util.XStream2;

import java.io.*;
import java.util.*;

/**
 * POJO allowing to store the PluginCompatTester report
 * @author Frederic Camblor
 */
public class PluginCompatReport {
    private Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests;
    private SortedSet<MavenCoordinates> testedCoreCoordinates;

    public PluginCompatReport(){
        this.pluginCompatTests = new TreeMap<PluginInfos, List<PluginCompatResult>>();
        this.testedCoreCoordinates = new TreeSet<MavenCoordinates>();
    }

    public void add(PluginInfos infos, PluginCompatResult result){
        if(!this.pluginCompatTests.containsKey(infos)){
            this.pluginCompatTests.put(infos, new ArrayList<PluginCompatResult>());
        }

        List<PluginCompatResult> results = pluginCompatTests.get(infos);
        // Deleting existing result if it exists
        if(results.contains(result)){
            results.remove(result);
        }
        results.add(result);

        // Updating maven testedMavenCoordinates
        if(!this.testedCoreCoordinates.contains(result.coreCoordinates)){
            this.testedCoreCoordinates.add(result.coreCoordinates);
        }
    }

    public void save(File reportPath) throws IOException {
        // Ensuring every PluginCompatResult list is sorted
        for(List<PluginCompatResult> results : this.pluginCompatTests.values()){
            Collections.sort(results);
        }

        // Writing to a temporary report file ...
        File tempReportPath = new File(reportPath.getAbsolutePath()+".tmp");
        Writer out = new FileWriter(tempReportPath);
        out.write(String.format("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>%n"));
        out.write(String.format("<?xml-stylesheet href=\""+getXslFilename(reportPath)+"\" type=\"text/xsl\"?>%n"));
        XStream xstream = createXStream();
        xstream.toXML(this, out);
        out.flush();
        out.close();

        // When everything went well, let's overwrite old report XML file with the new one
        //FileUtils.rename(tempReportPath, reportPath);
        reportPath.delete();
        tempReportPath.renameTo(reportPath);
    }

    public static String getXslFilename(File reportPath){
        return getBaseFilename(reportPath)+".xsl";
    }

    public static File getXslFilepath(File reportPath){
        return new File(getBaseFilepath(reportPath)+".xsl");
    }

    public static File getHtmlFilepath(File reportPath){
        return new File(getBaseFilepath(reportPath)+".html");
    }

    public static String getBaseFilepath(File reportPath){
        return reportPath.getParentFile().getAbsolutePath()+"/"+getBaseFilename(reportPath);
    }

    public static String getBaseFilename(File reportPath){
        return reportPath.getName().split("\\.")[0];
    }

    public boolean isCompatTestResultAlreadyInCache(PluginInfos pluginInfos, MavenCoordinates coreCoord, long cacheTimeout, TestStatus cacheThresholdStatus){
        // Retrieving plugin compat results corresponding to pluginsInfos + coreCoord
        if(!pluginCompatTests.containsKey(pluginInfos)){
            // No data for this plugin version ? => no cache !
            return false;
        }

        List<PluginCompatResult> results = pluginCompatTests.get(pluginInfos);
        PluginCompatResult resultCorrespondingToGivenCoreCoords = null;
        for(PluginCompatResult r : results){
            if(r.coreCoordinates.equals(coreCoord)){
                resultCorrespondingToGivenCoreCoords = r;
                break;
            }
        }
        if(resultCorrespondingToGivenCoreCoords == null){
            // No data for this core coordinates ? => no cache !
            return false;
        }

        // Is the latest execution on this plugin compliant with the given cache timeout ?
        // If so, then cache will be activated !
        if(new Date().before(new Date(resultCorrespondingToGivenCoreCoords.compatTestExecutedOn.getTime() + cacheTimeout))){
            return true;
        }

        // Status was lower than cacheThresholdStatus ? => no cache !
        return (!resultCorrespondingToGivenCoreCoords.status.isLowerThan(cacheThresholdStatus));
    }

    public static PluginCompatReport fromXml(File reportPath) {
        PluginCompatReport report = null;

        // Reading report file from reportPath
        XStream xstream = createXStream();
        try {
            report = (PluginCompatReport)xstream.fromXML(new FileInputStream(reportPath));
        } catch (FileNotFoundException e) {
            // Path doesn't exist => create a new report object
            report = new PluginCompatReport();
        }

        // Ensuring we are using a TreeMap for pluginCompatTests
        if(!(report.pluginCompatTests instanceof SortedMap)){
            TreeMap<PluginInfos, List<PluginCompatResult>> sortedMap = new TreeMap<PluginInfos, List<PluginCompatResult>>();
            sortedMap.putAll(report.pluginCompatTests);
            report.pluginCompatTests = sortedMap;
        }

        return report;
    }

    private static XStream2 createXStream(){
        XStream2 xstream = new XStream2(new Xpp3DomDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("pluginInfos", PluginInfos.class);
        xstream.alias("coord", MavenCoordinates.class);
        xstream.alias("compatResult", PluginCompatResult.class);
        xstream.alias("status", TestStatus.class);
        xstream.alias("report", PluginCompatReport.class);
        return xstream;
    }

    public SortedSet<MavenCoordinates> getTestedCoreCoordinates() {
        return new TreeSet(testedCoreCoordinates);
    }

    public Map<PluginInfos, List<PluginCompatResult>> getPluginCompatTests(){
        return new TreeMap<PluginInfos, List<PluginCompatResult>>(pluginCompatTests);
    }
}
