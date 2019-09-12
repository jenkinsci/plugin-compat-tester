package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fcamblor
 */
public class DAOUtils {
    public static List<Key> translateToKeyList(List<Entity> entities){
        List<Key> keys = new ArrayList<>(entities.size());
        for(Entity e : entities){
            keys.add(e.getKey());
        }
        return keys;
    }

    public static long purgeEntities(String kind){
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        long deletedLines = 0;
        List<Key> entityKeys = translateToKeyList(datastoreService.prepare(new Query(kind)).asList(FetchOptions.Builder.withLimit(10000)));
        datastoreService.delete(entityKeys);
        deletedLines += entityKeys.size();

        return deletedLines;
    }
}
