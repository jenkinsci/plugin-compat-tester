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

    public static String toJson(PluginCompatReport report){
        return String.format("{\"coreCoordinates\":%s,\"plugins\":%s,\"results\":%s}",
                toJson(report.getTestedCoreCoordinates()),
                toJson(report.getPluginCompatTests().keySet()),
                toJson(report.getPluginCompatTests()));
    }

    public static String toJson(Map<PluginInfos, List<PluginCompatResult>> pluginCompatTests) {
        StringBuilder sb = new StringBuilder("[");
        for(PluginInfos pi : pluginCompatTests.keySet()){
            sb.append(String.format("{\"plugin\":\"%s:%s\",\"results\":%s},", pi.pluginName, pi.pluginVersion, toJson(pluginCompatTests.get(pi))));
        }
        if(pluginCompatTests.keySet().size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJson(List<PluginCompatResult> pluginCompatResults) {
        // Ensuring results are sorted
        Collections.sort(pluginCompatResults);

        StringBuilder sb = new StringBuilder("[");
        for(PluginCompatResult res : pluginCompatResults){
            sb.append(String.format("{\"core\":\"%s\",\"status\":\"%s\",\"date\":%s,%s%s%s%s},",res.coreCoordinates.toGAV(),res.status.name(),
                    res.compatTestExecutedOn.getTime(), displayMessage("err", res.errorMessage), res.errorMessage==null?"":",",
                    displayMessages("warn", res.warningMessages), res.warningMessages==null?"":","));
        }
        if(pluginCompatResults.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
        return sb.toString();
    }

    public static String displayMessages(String label, Collection<String> messages) {
        if(messages==null){
            return "";
        }

        StringBuilder sb = new StringBuilder(String.format("\"%s\":[", label));
        for(String msg : messages){
            sb.append(String.format("%s,",displayMessage(null, msg)));
        }
        if(messages.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
        return sb.toString();
    }

    public static String displayMessage(String label, String errorMessage) {
        if(errorMessage == null){
            return "";
        }
        return String.format("%s\"%s\"", label==null?"":"\""+label+"\":", errorMessage.replaceAll("\"", "\\\\\"").replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
    }

    public static String toJson(Set<PluginInfos> pluginInfos) {
        StringBuilder sb = new StringBuilder("[");
        for(PluginInfos pi : pluginInfos){
            sb.append(String.format("{\"name\":\"%s\",\"version\":\"%s\",\"url\":\"%s\"},", pi.pluginName, pi.pluginVersion, pi.pluginUrl));
        }
        if(pluginInfos.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJson(SortedSet<MavenCoordinates> testedCoreCoordinates) {
        StringBuilder sb = new StringBuilder("[");
        for(MavenCoordinates coord : testedCoreCoordinates){
            sb.append(String.format("{\"g\":\"%s\",\"a\":\"%s\",\"v\":\"%s\"},", coord.groupId, coord.artifactId, coord.version));
        }
        if(testedCoreCoordinates.size()!=0){
            sb.deleteCharAt(sb.length()-1); // Removing last comma
        }
        sb.append("]");
        return sb.toString();
    }
}
