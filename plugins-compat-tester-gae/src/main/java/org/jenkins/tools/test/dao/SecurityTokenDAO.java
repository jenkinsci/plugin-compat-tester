package org.jenkins.tools.test.dao;

import com.google.appengine.api.datastore.*;

import java.util.logging.Logger;

/**
 * @author fcamblor
 */
public enum SecurityTokenDAO {
    INSTANCE;

    private static final Logger log = Logger.getLogger(PluginCompatResultDAO.class.getName());

    private static final String SECURITY_TOKEN_KIND = "securityToken";
    private static enum SecurityProperties {
        value, activated
    }

    public void initializeUniqueToken(){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        // Ensuring there isn't any token yet in DB...
        int tokenCount = datastore.prepare(
                new Query(SECURITY_TOKEN_KIND)
        ).asList(FetchOptions.Builder.withDefaults()).size();
        if(tokenCount != 0){
            throw new IllegalStateException(String.format(
                    "Attempting to create a new token whereas %d tokens are present in datastore !", tokenCount));
        }

        Entity token = new Entity(SECURITY_TOKEN_KIND);
        token.setProperty(SecurityProperties.value.name(), "xxxxx");
        // Created token is deactivated by default :
        // you will have to manually edit your generated token value / activated flag
        // in order to be able to use it !
        token.setProperty(SecurityProperties.activated.name(), false);

        Key tokenKey = datastore.put(token);
        log.info(String.format("Created token with key [%s] !", tokenKey.getId()));
    }

    public boolean isTokenValid(String token){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        int tokenCount = datastore.prepare(
                new Query(SECURITY_TOKEN_KIND)
                    .addFilter(SecurityProperties.value.name(), Query.FilterOperator.EQUAL, token)
                    .addFilter(SecurityProperties.activated.name(), Query.FilterOperator.EQUAL, true)
        ).asList(FetchOptions.Builder.withDefaults()).size();

        return tokenCount != 0;
    }
}
