package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;
import org.jenkins.tools.test.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author fcamblor
 */
public enum PluginCompatResultDAO {
    INSTANCE;

    private static final Logger log = Logger.getLogger(PluginCompatResultDAO.class.getName());

    public Key persist(PluginInfos pluginInfos, PluginCompatResult result){

        Key coreCoordinatesEntityKey = createCoreCoordsIfNotExist(result.coreCoordinates);
        Key pluginInfosEntityKey = createPluginInfosIfNotExist(pluginInfos);

        Entity resultToPersist = Mappings.toEntity(result, result.coreCoordinates, coreCoordinatesEntityKey, pluginInfos, pluginInfosEntityKey);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key resultKey = datastore.put(resultToPersist);

        log.info("Plugin compat result stored with key : "+resultKey);

        return resultKey;
    }

    private Key createPluginInfosIfNotExist(PluginInfos pluginInfos) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List<Entity> pluginInfosEntities = datastore.prepare(
                new Query(Mappings.PluginInfosProperties.KIND)
                    .addFilter(Mappings.PluginInfosProperties.pluginName.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginName)
                    .addFilter(Mappings.PluginInfosProperties.pluginVersion.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginVersion)
                    .addFilter(Mappings.PluginInfosProperties.pluginUrl.name(), Query.FilterOperator.EQUAL, pluginInfos.pluginUrl)
        ).asList(FetchOptions.Builder.withDefaults());

        Key result = null;
        if(pluginInfosEntities.size() == 0){
            // Coordinate doesn't exist : let's create it !
            Entity pluginInfoEntity = Mappings.toEntity(pluginInfos);
            result = datastore.put(pluginInfoEntity);
        } else {
            result = pluginInfosEntities.get(0).getKey();
        }

        return result;
    }

    private Key createCoreCoordsIfNotExist(MavenCoordinates coreCoords){
        return searchAndEventuallyCreateCoords(Mappings.CORE_MAVEN_COORDS_KIND, coreCoords);
    }

    private Key searchAndEventuallyCreateCoords(String kind, MavenCoordinates coords){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List<Entity> coordsEntities = datastore.prepare(
                new Query(kind)
                    .addFilter(Mappings.MavenCoordinatesProperties.gav.name(), Query.FilterOperator.EQUAL, coords.toGAV())
        ).asList(FetchOptions.Builder.withDefaults());

        Key result = null;
        if(coordsEntities.size() == 0){
            // Coordinate doesn't exist : let's create it !
            Entity coordsEntity = Mappings.toEntity(coords, kind);
            result = datastore.put(coordsEntity);
        } else {
            result = coordsEntities.get(0).getKey();
        }

        return result;
    }
}
