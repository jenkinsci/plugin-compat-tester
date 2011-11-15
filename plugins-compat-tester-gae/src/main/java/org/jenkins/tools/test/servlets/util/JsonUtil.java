package org.jenkins.tools.test.servlets.util;

import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatReport;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;

import java.util.*;

/**
 * @author fcamblor
 */
public class JsonUtil {

    public static void toJson(StringBuilder sb, PluginCompatReport report){
        sb.append("{\"coreCoordinates\":");
        toJson(sb, report.getTestedCoreCoordinates());
        sb.append(",\"plugins\":");
        toJson(sb, report.getPluginCompatTests().keySet());
        sb.append(",\"results\":");
        toJson(sb, report.getPluginCompatTests());
        sb.append("}");
    }

    public static void toJson(StringBuilder sb, Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests) {
        sb.append("[");
        for(PluginInfos pi : pluginCompatTests.keySet()){
            sb.append(String.format("{\"plugin\":\"%s:%s\",\"results\":", pi.pluginName, pi.pluginVersion));
            toJson(sb, pluginCompatTests.get(pi));
            sb.append("},");
        }
        if(pluginCompatTests.keySet().size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
    }

    public static void toJson(StringBuilder sb, List<PluginCompatResult> pluginCompatResults) {
        // Ensuring results are sorted
        Collections.sort(pluginCompatResults);

        sb.append("[");
        for(PluginCompatResult res : pluginCompatResults){
            sb.append("{\"core\":\"");
            sb.append(res.coreCoordinates.toGAV());
            sb.append("\",\"status\":\"");
            sb.append(res.status.name());
            sb.append("\",\"date\":\"");
            sb.append(res.compatTestExecutedOn.getTime());
            sb.append("\",\"buildLogPath\":\"");
            sb.append(res.getBuildLogPath()==null?"":res.getBuildLogPath());
            sb.append("\",");
            sb.append(displayMessage("err", res.errorMessage));
            sb.append(res.errorMessage==null?"":",");

            displayMessages(sb, "warn", res.warningMessages);
            sb.append(res.warningMessages==null?"":",");
            sb.append("},");

            sb.deleteCharAt(sb.length() - 3); // Removing last comma
        }
        if(pluginCompatResults.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
    }

    public static void displayMessages(StringBuilder sb, String label, Collection<String> messages) {
        if(messages==null){
            return;
        }

        sb.append(String.format("\"%s\":[", label));
        for(String msg : messages){
            sb.append(String.format("%s,",displayMessage(null, msg)));
        }
        if(messages.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
    }

    public static String displayMessage(String label, String message) {
        if(message == null){
            return "";
        }

        message = message.replaceAll("\"", "\\\\\"").replaceAll("\r", "\\\\r")
                         .replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
        return String.format("%s\"%s\"", label==null?"":"\""+label+"\":", message);
    }

    public static void toJson(StringBuilder sb, Set<PluginInfos> pluginInfos) {
        sb.append("[");
        for(PluginInfos pi : pluginInfos){
            sb.append(String.format("{\"name\":\"%s\",\"version\":\"%s\",\"url\":\"%s\"},", pi.pluginName, pi.pluginVersion, pi.pluginUrl));
        }
        if(pluginInfos.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
    }

    public static void toJson(StringBuilder sb, SortedSet<MavenCoordinates> testedCoreCoordinates) {
        sb.append("[");
        for(MavenCoordinates coord : testedCoreCoordinates){
            sb.append(String.format("{\"g\":\"%s\",\"a\":\"%s\",\"v\":\"%s\"},", coord.groupId, coord.artifactId, coord.version));
        }
        if(testedCoreCoordinates.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
    }
}
