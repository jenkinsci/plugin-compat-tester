package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import org.jenkins.tools.test.model.*;

import java.util.*;

/**
 * @author fcamblor
 */
public class Mappings {
    public static enum PluginCompatResultProperties {
        compatTestExecutedOn, status, errorMessage, warningMessages,
        coreCoordsKey, pluginInfosKey, computedCoreAndPlugin;
        public static final String KIND = "pluginCompatResult";
    }
    public static enum PluginInfosProperties {
        pluginName, pluginVersion, pluginUrl;
        public static final String KIND = "pluginInfos";
    }
    public static enum MavenCoordinatesProperties {
        gav, groupId, artifactId, version;
    }
    public static final String CORE_MAVEN_COORDS_KIND = "coreCoordinates";

    public static Entity toEntity(PluginCompatResult result, MavenCoordinates coreCoordinates, Key coreCoordinatesEntityKey,
                                  PluginInfos pluginInfos, Key pluginInfosEntityKey){
        Entity resultEntity = new Entity(PluginCompatResultProperties.KIND);
        resultEntity.setProperty(PluginCompatResultProperties.coreCoordsKey.name(), coreCoordinatesEntityKey);
        resultEntity.setProperty(PluginCompatResultProperties.pluginInfosKey.name(), pluginInfosEntityKey);
        resultEntity.setProperty(PluginCompatResultProperties.computedCoreAndPlugin.name(), computeCoreAndPlugin(coreCoordinates, pluginInfos));
        resultEntity.setProperty(PluginCompatResultProperties.status.name(), result.status.toString());
        resultEntity.setProperty(PluginCompatResultProperties.compatTestExecutedOn.name(), result.compatTestExecutedOn);
        if(result.errorMessage == null){
            resultEntity.setProperty(PluginCompatResultProperties.errorMessage.name(), null);
        }else{
            resultEntity.setProperty(PluginCompatResultProperties.errorMessage.name(), new Text(result.errorMessage));
        }

        // Transforming warning messages into text
        List<Text> textWarnMsg = null;
        if(result.warningMessages != null){
            textWarnMsg = new ArrayList<Text>();
            for(String warnMsg : result.warningMessages){
                if(warnMsg == null){
                    textWarnMsg.add(null);
                }else{
                    textWarnMsg.add(new Text(warnMsg));
                }
            }
        }
        resultEntity.setProperty(PluginCompatResultProperties.warningMessages.name(), textWarnMsg);
        return resultEntity;
    }

    public static String computeCoreAndPlugin(MavenCoordinates coreCoords, PluginInfos pluginInfos){
        return pluginInfos.pluginName+"_"+coreCoords.toGAV();
    }

    public static Entity toEntity(PluginInfos pluginInfos){
        Entity pluginInfoEntity = new Entity(PluginInfosProperties.KIND);
        pluginInfoEntity.setProperty(PluginInfosProperties.pluginName.name(), pluginInfos.pluginName);
        pluginInfoEntity.setProperty(PluginInfosProperties.pluginVersion.name(), pluginInfos.pluginVersion);
        pluginInfoEntity.setProperty(PluginInfosProperties.pluginUrl.name(), pluginInfos.pluginUrl);
        return pluginInfoEntity;
    }

    public static Entity toEntity(MavenCoordinates coords, String kind){
        Entity coordsEntity = new Entity(kind);
        coordsEntity.setProperty(MavenCoordinatesProperties.gav.name(), coords.toGAV());
        coordsEntity.setProperty(MavenCoordinatesProperties.groupId.name(), coords.groupId);
        coordsEntity.setProperty(MavenCoordinatesProperties.artifactId.name(), coords.artifactId);
        coordsEntity.setProperty(MavenCoordinatesProperties.version.name(), coords.version);
        return coordsEntity;
    }
}
