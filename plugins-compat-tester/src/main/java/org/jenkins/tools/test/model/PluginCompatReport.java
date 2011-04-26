package org.jenkins.tools.test.model;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.FileUtils;
import org.jenkins.tools.test.model.comparators.MavenCoordinatesComparator;

import java.io.*;
import java.util.*;

public class PluginCompatReport {
    private Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests;
    private SortedSet<MavenCoordinates> testedCoreCoordinates;

    public PluginCompatReport(){
        this.pluginCompatTests = new HashMap<PluginInfos, List<PluginCompatResult>>();
        this.testedCoreCoordinates = new TreeSet<MavenCoordinates>(new MavenCoordinatesComparator());
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
        FileUtils.rename(tempReportPath, reportPath);
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

    public boolean isCompatTestResultAlreadyInCache(PluginInfos pluginInfos, MavenCoordinates coreCoord, long cacheTimeout){
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
        return new Date().before(new Date(resultCorrespondingToGivenCoreCoords.compatTestExecutedOn.getTime() + cacheTimeout));
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

        return report;
    }

    private static XStream createXStream(){
        XStream xstream = new XStream();
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("pluginInfos", PluginInfos.class);
        xstream.alias("coord", MavenCoordinates.class);
        xstream.alias("compatResult", PluginCompatResult.class);
        xstream.alias("status", TestStatus.class);
        xstream.alias("report", PluginCompatReport.class);
        return xstream;
    }

    public SortedSet<MavenCoordinates> getTestedCoreCoordinates() {
        return Collections.unmodifiableSortedSet(testedCoreCoordinates);
    }
}
