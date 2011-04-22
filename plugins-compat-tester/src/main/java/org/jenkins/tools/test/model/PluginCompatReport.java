package org.jenkins.tools.test.model;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PluginCompatReport {
    private HashMap<String, List<PluginCompatResult>> pluginCompatTests;

    public PluginCompatReport(){
        this.pluginCompatTests = new HashMap<String, List<PluginCompatResult>>();
    }

    public void add(String pluginName, PluginCompatResult result){
        if(!this.pluginCompatTests.containsKey(pluginName)){
            this.pluginCompatTests.put(pluginName, new ArrayList<PluginCompatResult>());
        }

        List<PluginCompatResult> results = pluginCompatTests.get(pluginName);
        // Deleting existing result if it exists
        if(results.contains(result)){
            results.remove(result);
        }
        results.add(result);
    }

    public void save(File reportPath) throws FileNotFoundException {
        XStream xstream = createXStream();
        xstream.toXML(this, new FileOutputStream(reportPath));
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
        xstream.alias("coord", MavenCoordinates.class);
        xstream.alias("compatResult", PluginCompatResult.class);
        xstream.alias("report", PluginCompatReport.class);
        return xstream;
    }
}
