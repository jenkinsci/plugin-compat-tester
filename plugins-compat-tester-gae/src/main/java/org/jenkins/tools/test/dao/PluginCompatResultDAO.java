package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;
import org.jenkins.tools.test.model.MavenCoordinates;
import org.jenkins.tools.test.model.PluginCompatResult;
import org.jenkins.tools.test.model.PluginInfos;
import org.jenkins.tools.test.model.TestStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author fcamblor
 */
public enum PluginCompatResultDAO {
    INSTANCE;

    private static enum PluginCompatResultProperties {
        coreCoordsKey, pluginInfosKey, compatTestExecutedOn, status, errorMessage, warningMessages;
        public static final String KIND = "pluginCompatResult";
    }
    private static enum PluginInfosProperties {
        pluginName, pluginVersion, pluginUrl;
        public static final String KIND = "pluginInfos";
    }
    private static enum MavenCoordinatesProperties {
        groupId, artifactId, version;
    }
    public static final String CORE_MAVEN_COORDS_KIND = "coreCoordinates";

    private static final Logger log = Logger.getLogger(PluginCompatResultDAO.class.getName());

    public Key persist(PluginInfos pluginInfos, PluginCompatResult result){

        Key coreCoordinatesEntityKey = createCoreCoordsIfNotExist(result.coreCoordinates);
        Key pluginInfosEntityKey = createPluginInfosIfNotExist(pluginInfos);

        Entity resultToPersist = new Entity(PluginCompatResultProperties.KIND);
        resultToPersist.setProperty(PluginCompatResultProperties.coreCoordsKey.name(), coreCoordinatesEntityKey);
        resultToPersist.setProperty(PluginCompatResultProperties.pluginInfosKey.name(), pluginInfosEntityKey);
        resultToPersist.setProperty(PluginCompatResultProperties.status.name(), result.status.toString());
        resultToPersist.setProperty(PluginCompatResultProperties.compatTestExecutedOn.name(), result.compatTestExecutedOn);
        if(result.errorMessage == null){
            resultToPersist.setProperty(PluginCompatResultProperties.errorMessage.name(), null);
        }else{
            resultToPersist.setProperty(PluginCompatResultProperties.errorMessage.name(), new Text(result.errorMessage));
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
        resultToPersist.setProperty(PluginCompatResultProperties.warningMessages.name(), textWarnMsg);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key resultKey = datastore.put(resultToPersist);

        log.info("Plugin compat result stored with key : "+resultKey);

        return resultKey;
    }

    private Key createPluginInfosIfNotExist(PluginInfos pluginInfos) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List<Entity> pluginInfosEntities = datastore.prepare(
                new Query(PluginInfosProperties.KIND)
                    .addFilter(PluginInfosProperties.pluginName.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginName)
                    .addFilter(PluginInfosProperties.pluginVersion.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginVersion)
                    .addFilter(PluginInfosProperties.pluginUrl.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginUrl)
        ).asList(FetchOptions.Builder.withDefaults());

        Key result = null;
        if(pluginInfosEntities.size() == 0){
            // Coordinate doesn't exist : let's create it !
            Entity pluginInfoEntity = new Entity(PluginInfosProperties.KIND);
            pluginInfoEntity.setProperty(PluginInfosProperties.pluginName.name(), pluginInfos.pluginName);
            pluginInfoEntity.setProperty(PluginInfosProperties.pluginVersion.name(), pluginInfos.pluginVersion);
            pluginInfoEntity.setProperty(PluginInfosProperties.pluginUrl.name(), pluginInfos.pluginUrl);

            result = datastore.put(pluginInfoEntity);
        } else {
            result = pluginInfosEntities.get(0).getKey();
        }

        return result;
    }

    private Key createCoreCoordsIfNotExist(MavenCoordinates coreCoords){
        return searchAndEventuallyCreateCoords(CORE_MAVEN_COORDS_KIND, coreCoords);
    }

    private Key searchAndEventuallyCreateCoords(String kind, MavenCoordinates coords){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List<Entity> coordsEntities = datastore.prepare(
                new Query(kind)
                    .addFilter(MavenCoordinatesProperties.groupId.name(), Query.FilterOperator.EQUAL, coords.groupId)
                    .addFilter(MavenCoordinatesProperties.artifactId.name(), Query.FilterOperator.EQUAL, coords.artifactId)
                    .addFilter(MavenCoordinatesProperties.version.name(), Query.FilterOperator.EQUAL, coords.version)
        ).asList(FetchOptions.Builder.withDefaults());

        Key result = null;
        if(coordsEntities.size() == 0){
            // Coordinate doesn't exist : let's create it !
            Entity coordsEntity = new Entity(kind);
            coordsEntity.setProperty(MavenCoordinatesProperties.groupId.name(), coords.groupId);
            coordsEntity.setProperty(MavenCoordinatesProperties.artifactId.name(), coords.artifactId);
            coordsEntity.setProperty(MavenCoordinatesProperties.version.name(), coords.version);

            result = datastore.put(coordsEntity);
        } else {
            result = coordsEntities.get(0).getKey();
        }

        return result;
    }
}
