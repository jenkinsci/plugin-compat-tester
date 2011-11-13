package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;
import org.jenkins.tools.test.model.PluginInfos;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author fcamblor
 */
public enum LogsDAO {
    INSTANCE;

    private static final Logger log = Logger.getLogger(LogsDAO.class.getName());

    public Key persistBuildLog(String buildLogPath, String logContent, Key pluginCompatResult){
        Entity logToPersist = Mappings.toEntity(buildLogPath, logContent, pluginCompatResult);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key resultKey = datastore.put(logToPersist);

        log.info("Log stored with key : "+resultKey);

        return resultKey;
    }

    public String getLogContent(String buildPath){
        return getLogContentBasedOnCriteria(Mappings.LogProperties.buildLogPath.name(), buildPath);
    }

    public String getLogContent(Key key){
        return getLogContentBasedOnCriteria(Mappings.LogProperties.resultKey.name(), key);
    }

    private String getLogContentBasedOnCriteria(String property, Object value){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query searchLogQuery = new Query(Mappings.LogProperties.KIND)
                .addFilter(property, Query.FilterOperator.EQUAL, value);
        Entity log = datastore.prepare(searchLogQuery).asSingleEntity();
        return Mappings.logContentFromEntity(log);
    }

    public long purgeResults() {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        long deletedLines = 0;

        deletedLines += DAOUtils.purgeEntities(Mappings.LogProperties.KIND);

        return deletedLines;
    }
}
