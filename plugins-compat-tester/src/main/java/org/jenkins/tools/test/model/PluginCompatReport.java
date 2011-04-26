package org.jenkins.tools.test.model;

import com.thoughtworks.xstream.XStream;
import org.jenkins.tools.test.model.comparators.MavenCoordinatesComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

public class PluginCompatReport {
    private Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests;
    private Set<MavenCoordinates> testedCoreCoordinates;

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
        if(!testedCoreCoordinates.contains(result.coreCoordinates)){
            testedCoreCoordinates.add(result.coreCoordinates);
        }
    }

    public void save(File reportPath) throws FileNotFoundException {
        XStream xstream = createXStream();
        xstream.toXML(this, new FileOutputStream(reportPath));
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
        return new Date().before(new Date(resultCorrespondingToGivenCoreCoords.compatTestExecutedOn.getTime()+cacheTimeout));
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
        xstream.alias("report", PluginCompatReport.class);
        return xstream;
    }

}
