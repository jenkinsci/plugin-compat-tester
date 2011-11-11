package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fcamblor
 */
public class Mappings {
    public static enum PluginCompatResultProperties {
        coreCoordsKey, pluginInfosKey, compatTestExecutedOn, status, errorMessage, warningMessages;
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

    public static Entity toEntity(PluginCompatResult result, Key coreCoordinatesEntityKey, Key pluginInfosEntityKey){
        Entity resultEntity = new Entity(PluginCompatResultProperties.KIND);
        resultEntity.setProperty(PluginCompatResultProperties.coreCoordsKey.name(), coreCoordinatesEntityKey);
        resultEntity.setProperty(PluginCompatResultProperties.pluginInfosKey.name(), pluginInfosEntityKey);
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
