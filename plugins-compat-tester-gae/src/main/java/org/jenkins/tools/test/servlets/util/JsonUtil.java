package org.jenkins.tools.test.servlets.util;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * @author fcamblor
 */
public class JsonUtil {

    public static void toJson(Writer w, PluginCompatReport report) throws IOException {
        w.write("{\"coreCoordinates\":");
        toJson(w, report.getTestedCoreCoordinates());
        w.write(",\"plugins\":");
        toJson(w, report.getPluginCompatTests().keySet());
        w.write(",\"results\":");
        toJson(w, report.getPluginCompatTests());
        w.write("}");
    }

    public static void toJson(Writer w, Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests) throws IOException {
        w.write("[");
        for(Iterator<PluginInfos> piIter = pluginCompatTests.keySet().iterator(); piIter.hasNext();){
            PluginInfos pi = piIter.next();
            w.write(String.format("{\"plugin\":\"%s:%s\",\"results\":", pi.pluginName, pi.pluginVersion));
            toJson(w, pluginCompatTests.get(pi));
            w.write("}");
            if(piIter.hasNext()){
                w.write(",");
            }
        }
        w.write("]");
    }

    public static void toJson(Writer w, List<PluginCompatResult> pluginCompatResults) throws IOException {
        // Ensuring results are sorted
        Collections.sort(pluginCompatResults);

        w.write("[");
        for(Iterator<PluginCompatResult> resIter = pluginCompatResults.iterator(); resIter.hasNext();){
            PluginCompatResult res = resIter.next();
            w.write("{\"core\":\"");
            w.write(res.coreCoordinates.toGAV());
            w.write("\",\"status\":\"");
            w.write(res.status.name());
            w.write("\",\"date\":\"");
            w.write(String.valueOf(res.compatTestExecutedOn.getTime()));
            w.write("\",\"buildLogPath\":\"");
            w.write(res.getBuildLogPath() == null ? "" : res.getBuildLogPath());
            w.write("\"");
            if(res.errorMessage != null && !"".equals(res.errorMessage)){
                w.write(",");
                displayMessage(w, "err", res.errorMessage);
            }

            if(res.warningMessages != null && !res.warningMessages.isEmpty()){
                w.write(",");
                displayMessages(w, "warn", res.warningMessages);
            }
            w.write("}");

            if(resIter.hasNext()){
                w.write(",");
            }
        }
        w.write("]");
    }

    public static void displayMessages(Writer w, String label, Collection<String> messages) throws IOException {
        if(messages==null){
            return;
        }

        w.write(String.format("\"%s\":[", label));
        for(Iterator<String> msgIter = messages.iterator(); msgIter.hasNext();){
            String msg = msgIter.next();
            displayMessage(w, null, msg);
            if(msgIter.hasNext()){
                w.write(",");
            }
        }
        w.write("]");
    }

    public static void displayMessage(Writer w, String label, String message) throws IOException {
        if(message == null){
            return;
        }

        message = message.replaceAll("\"", "\\\\\"").replaceAll("\r", "\\\\r")
                         .replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
        w.write(label==null?"":"\""+label+"\":");
        w.write("\"");
        w.write(message);
        w.write("\"");
    }

    public static void toJson(Writer w, Set<PluginInfos> pluginInfos) throws IOException {
        w.write("[");
        for(Iterator<PluginInfos> piIter = pluginInfos.iterator(); piIter.hasNext();){
            PluginInfos pi = piIter.next();
            w.write(String.format("{\"name\":\"%s\",\"version\":\"%s\",\"url\":\"%s\"}", pi.pluginName, pi.pluginVersion, pi.pluginUrl));
            if(piIter.hasNext()){
                w.write(",");
            }
        }
        w.write("]");
    }

    public static void toJson(Writer w, SortedSet<MavenCoordinates> testedCoreCoordinates) throws IOException {
        w.write("[");
        for(Iterator<MavenCoordinates> coordIter = testedCoreCoordinates.iterator(); coordIter.hasNext();){
            MavenCoordinates coord = coordIter.next();
            w.write(String.format("{\"g\":\"%s\",\"a\":\"%s\",\"v\":\"%s\"}", coord.groupId, coord.artifactId, coord.version));
            if(coordIter.hasNext()){
                w.write(",");
            }
        }
        w.write("]");
    }
}
