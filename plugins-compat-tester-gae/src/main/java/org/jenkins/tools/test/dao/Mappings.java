package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import org.jenkins.tools.test.model.*;
import org.jenkins.tools.test.model.utils.IOUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author fcamblor
 */
public class Mappings {
    public enum PluginCompatResultProperties {
        compatTestExecutedOn, status, errorMessage, warningMessages, buildLogPath,
        coreCoordsKey, pluginInfosKey, computedCoreAndPlugin;
        public static final String KIND = "pluginCompatResult";
    }
    public enum PluginInfosProperties {
        pluginName, pluginVersion, pluginUrl;
        public static final String KIND = "pluginInfos";
    }
    public enum MavenCoordinatesProperties {
        gav, groupId, artifactId, version
    }
    public static final String CORE_MAVEN_COORDS_KIND = "coreCoordinates";
    public enum LogProperties {
        resultKey, buildLogPath, logContent;
        public static final String KIND = "logs";
    }

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
        if(result.getBuildLogPath() == null){
            resultEntity.setProperty(PluginCompatResultProperties.buildLogPath.name(), null);
        }else{
            resultEntity.setProperty(PluginCompatResultProperties.buildLogPath.name(), result.getBuildLogPath());
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

    public static PluginCompatResult pluginCompatResultFromEntity(Entity entity, Map<Key, MavenCoordinates> cores) {

        MavenCoordinates coreCoords = cores.get((Key)entity.getProperty(PluginCompatResultProperties.coreCoordsKey.name()));
        TestStatus status = TestStatus.valueOf((String)entity.getProperty(PluginCompatResultProperties.status.name()));
        Date compatTestExecutedOn = (Date)entity.getProperty(PluginCompatResultProperties.compatTestExecutedOn.name());
        String buildLogPathStr = (String)entity.getProperty(PluginCompatResultProperties.buildLogPath.name());
        Text errMsg = (Text)entity.getProperty(PluginCompatResultProperties.errorMessage.name());
        String errMsgStr = null;
        if(errMsg != null){
            errMsgStr = errMsg.getValue();
        }
        List<Text> warnMsgs = (List<Text>)entity.getProperty(PluginCompatResultProperties.warningMessages.name());
        // Transforming warning messages from text
        List<String> strWarnMsg = null;
        if(warnMsgs != null){
            strWarnMsg = new ArrayList<String>();
            for(Text warnMsg : warnMsgs){
                if(warnMsg == null){
                    strWarnMsg.add(null);
                }else{
                    strWarnMsg.add(warnMsg.getValue());
                }
            }
        }

        return new PluginCompatResult(coreCoords, status, errMsgStr, strWarnMsg, buildLogPathStr, compatTestExecutedOn);
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

    public static PluginInfos pluginInfosFromEntity(Entity entity){
        return new PluginInfos(
                (String)entity.getProperty(PluginInfosProperties.pluginName.name()),
                (String)entity.getProperty(PluginInfosProperties.pluginVersion.name()),
                (String)entity.getProperty(PluginInfosProperties.pluginUrl.name()));
    }

    public static Map<Key, PluginInfos> pluginInfosFromEntity(List<Entity> entities){
        Map<Key, PluginInfos> pluginInfos = new HashMap<Key, PluginInfos> (entities.size());
        for(Entity e : entities){
            pluginInfos.put(e.getKey(), pluginInfosFromEntity(e));
        }
        return pluginInfos;
    }

    public static Entity toEntity(MavenCoordinates coords, String kind){
        Entity coordsEntity = new Entity(kind);
        coordsEntity.setProperty(MavenCoordinatesProperties.gav.name(), coords.toGAV());
        coordsEntity.setProperty(MavenCoordinatesProperties.groupId.name(), coords.groupId);
        coordsEntity.setProperty(MavenCoordinatesProperties.artifactId.name(), coords.artifactId);
        coordsEntity.setProperty(MavenCoordinatesProperties.version.name(), coords.version);
        return coordsEntity;
    }

    public static MavenCoordinates mavenCoordsFromEntity(Entity entity){
        return MavenCoordinates.fromGAV((String) entity.getProperty(MavenCoordinatesProperties.gav.name()));
    }

    public static Map<Key, MavenCoordinates> mavenCoordsFromEntity(List<Entity> entities){
        Map<Key, MavenCoordinates> coords = new HashMap<Key, MavenCoordinates> (entities.size());
        for(Entity e : entities){
            coords.put(e.getKey(), mavenCoordsFromEntity(e));
        }
        return coords;
    }

    public static PluginCompatReport pluginCompatReportFromResultsEntities(List<Entity> results, Map<Key, MavenCoordinates> cores, Map<Key, PluginInfos> pluginInfos) {
        PluginCompatReport report = new PluginCompatReport();
        for(Entity e : results){
            PluginCompatResult result = pluginCompatResultFromEntity(e, cores);
            PluginInfos pi = pluginInfos.get((Key)e.getProperty(PluginCompatResultProperties.pluginInfosKey.name()));

            report.add(pi, result);
        }

        return report;
    }

    public static Entity toEntity(String buildLogPath, String logContent, Key pluginCompatResultKey) {
        Entity logEntity = new Entity(LogProperties.KIND);
        logEntity.setProperty(LogProperties.buildLogPath.name(), buildLogPath);
        logEntity.setProperty(LogProperties.logContent.name(), new Text(logContent));
        logEntity.setProperty(LogProperties.resultKey.name(), pluginCompatResultKey);
        return logEntity;
    }

    public static String logContentFromEntity(Entity log) {
        String compressedLogContent = ((Text)log.getProperty(LogProperties.logContent.name())).getValue();
        String logContent = null;
        try {
            logContent = IOUtils.gunzipString(compressedLogContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return logContent;
    }
}
