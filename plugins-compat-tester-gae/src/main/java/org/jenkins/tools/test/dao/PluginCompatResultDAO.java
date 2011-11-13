package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;
import org.jenkins.tools.test.model.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author fcamblor
 */
public enum PluginCompatResultDAO {
    INSTANCE;

    public static interface CoreMatcher {

        Query enhanceSearchCoreQuery(Query query);
        Query enhanceSearchResultQuery(Query query, Map<Key, MavenCoordinates> coords);

        public static enum All implements CoreMatcher {
            INSTANCE;
            public Query enhanceSearchCoreQuery(Query query){ return query; }
            public Query enhanceSearchResultQuery(Query query, Map<Key, MavenCoordinates> coords){ return query; }
        }

        public static class Parameterized implements CoreMatcher {
            private List<MavenCoordinates> cores;
            public Parameterized(List<MavenCoordinates> cores){
                this.cores = cores;
            }

            public Query enhanceSearchCoreQuery(Query query){
                if(this.cores.size() == 0){
                    return query;
                }

                List<String> gavs = new ArrayList<String>(cores.size());
                for(MavenCoordinates coord : cores){
                    gavs.add(coord.toGAV());
                }
                return query.addFilter(Mappings.MavenCoordinatesProperties.gav.name(), Query.FilterOperator.IN, gavs);
            }

            public Query enhanceSearchResultQuery(Query query, Map<Key, MavenCoordinates> coords){
                if(coords.size() == 0){
                    return query;
                } else {
                    return query.addFilter(Mappings.PluginCompatResultProperties.coreCoordsKey.name(),
                            Query.FilterOperator.IN, new ArrayList<Key>(coords.keySet()));
                }
            }
        }
    }
    public static interface PluginMatcher {

        public Query enhanceSearchPluginQuery(Query query);
        public Query enhanceSearchResultQuery(Query query, Map<Key, PluginInfos> pluginInfos);

        public static enum All implements PluginMatcher {
            INSTANCE;
            public Query enhanceSearchPluginQuery(Query query){ return query; }
            public Query enhanceSearchResultQuery(Query query, Map<Key, PluginInfos> pluginInfos){ return query; }
        }

        public static class Parameterized implements PluginMatcher {
            private List<String> pluginNames;
            public Parameterized(List<String> pluginNames){
                this.pluginNames = pluginNames;
            }
            public Query enhanceSearchPluginQuery(Query query){
                if(this.pluginNames.size() == 0){
                    return query;
                }

                return query.addFilter(Mappings.PluginInfosProperties.pluginName.name(), Query.FilterOperator.IN, pluginNames);
            }
            public Query enhanceSearchResultQuery(Query query, Map<Key, PluginInfos> pluginInfos){
                if(pluginInfos.size() == 0){
                    return query;
                } else {
                    return query.addFilter(Mappings.PluginCompatResultProperties.pluginInfosKey.name(),
                            Query.FilterOperator.IN, new ArrayList<Key>(pluginInfos.keySet()));
                }
            }
        }
    }

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

    public PluginCompatReport search(PluginMatcher pluginMatcher, CoreMatcher coreMatcher){

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query searchCoresQuery = new Query(Mappings.CORE_MAVEN_COORDS_KIND);
        searchCoresQuery = coreMatcher.enhanceSearchCoreQuery(searchCoresQuery);
        List<Entity> coreEntities = datastore.prepare(searchCoresQuery).asList(FetchOptions.Builder.withLimit(10000));
        Map<Key, MavenCoordinates> cores = Mappings.mavenCoordsFromEntity(coreEntities);

        Query searchPluginsQuery = new Query(Mappings.PluginInfosProperties.KIND);
        searchPluginsQuery = pluginMatcher.enhanceSearchPluginQuery(searchPluginsQuery);
        List<Entity> pluginInfoEntities = datastore.prepare(searchPluginsQuery).asList(FetchOptions.Builder.withLimit(10000));
        Map<Key, PluginInfos> pluginInfos = Mappings.pluginInfosFromEntity(pluginInfoEntities);

        Query searchResultsQuery = new Query(Mappings.PluginCompatResultProperties.KIND);
        if((pluginMatcher == PluginMatcher.All.INSTANCE && coreMatcher == CoreMatcher.All.INSTANCE)
            || (pluginMatcher == PluginMatcher.All.INSTANCE || coreMatcher == CoreMatcher.All.INSTANCE)){

            coreMatcher.enhanceSearchResultQuery(searchResultsQuery, cores);
            pluginMatcher.enhanceSearchResultQuery(searchResultsQuery, pluginInfos);

        } else {
            // Yeah that's not really well object oriented...
            searchResultsQuery.addFilter(Mappings.PluginCompatResultProperties.computedCoreAndPlugin.name(),
                    Query.FilterOperator.IN, cartesianProductOfCoreAndPlugins(cores, pluginInfos));
        }
        List<Entity> results = datastore.prepare(searchResultsQuery).asList(FetchOptions.Builder.withLimit(10000));
        PluginCompatReport report = Mappings.pluginCompatReportFromResultsEntities(results, cores, pluginInfos);

        return report;
    }

    public SortedSet<MavenCoordinates> findAllCores(){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query searchCoresQuery = new Query(Mappings.CORE_MAVEN_COORDS_KIND);
        List<Entity> coreEntities = datastore.prepare(searchCoresQuery).asList(FetchOptions.Builder.withLimit(10000));
        Map<Key, MavenCoordinates> cores = Mappings.mavenCoordsFromEntity(coreEntities);

        return new TreeSet<MavenCoordinates>(cores.values());
    }

    public SortedSet<String> findAllPluginInfoNames(){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query searchPluginInfosQuery = new Query(Mappings.PluginInfosProperties.KIND);
        List<Entity> pluginInfosEntities = datastore.prepare(searchPluginInfosQuery).asList(FetchOptions.Builder.withLimit(10000));
        Map<Key, PluginInfos> pluginInfos = Mappings.pluginInfosFromEntity(pluginInfosEntities);

        SortedSet<String> names = new TreeSet<String>(new Comparator<String>() {
            public int compare(String s, String s1) {
                return s.compareToIgnoreCase(s1);
            }
        });
        for(PluginInfos pi : pluginInfos.values()){
            names.add(pi.pluginName);
        }

        return names;
    }

    public long purgeResults(){
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        long deletedLines = 0;

        List<Key> coreKeys = translateToKeyList(datastoreService.prepare(new Query(Mappings.CORE_MAVEN_COORDS_KIND)).asList(FetchOptions.Builder.withLimit(10000)));
        datastoreService.delete(coreKeys);
        deletedLines += coreKeys.size();

        List<Key> pluginInfosKeys = translateToKeyList(datastoreService.prepare(new Query(Mappings.PluginInfosProperties.KIND)).asList(FetchOptions.Builder.withLimit(10000)));
        datastoreService.delete(pluginInfosKeys);
        deletedLines += pluginInfosKeys.size();

        List<Key> resultKeys = translateToKeyList(datastoreService.prepare(new Query(Mappings.PluginCompatResultProperties.KIND)).asList(FetchOptions.Builder.withLimit(10000)));
        datastoreService.delete(resultKeys);
        deletedLines += resultKeys.size();

        return deletedLines;
    }

    private List<Key> translateToKeyList(List<Entity> entities){
        List<Key> keys = new ArrayList<Key>(entities.size());
        for(Entity e : entities){
            keys.add(e.getKey());
        }
        return keys;
    }

    private List<String> cartesianProductOfCoreAndPlugins(Map<Key, MavenCoordinates> cores, Map<Key, PluginInfos> pluginInfos) {
        List<String> computedCoreAndPlugins = new ArrayList<String>(cores.values().size() * pluginInfos.values().size());
        for(MavenCoordinates coords : cores.values()){
            for(PluginInfos pi : pluginInfos.values()){
                computedCoreAndPlugins.add(Mappings.computeCoreAndPlugin(coords, pi));
            }
        }
        return computedCoreAndPlugins;
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
