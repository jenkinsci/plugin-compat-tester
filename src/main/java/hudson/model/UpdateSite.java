package hudson.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.sf.json.JSONObject;

public final class UpdateSite {

    /** In-memory representation of the update center data. */
    public static final class Data {
        /** The {@link UpdateSite} ID. */
        @NonNull public final String sourceId;

        /** The latest jenkins.war. */
        @NonNull public final Entry core;

        /** Plugins in the repository, keyed by their artifact IDs. */
        @NonNull
        public final Map<String, Plugin> plugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Data(JSONObject o) {
            this.sourceId = o.getString("id");
            this.core = new Entry(sourceId, o.getJSONObject("core"));
            for (Map.Entry<String, JSONObject> e :
                    (Set<Map.Entry<String, JSONObject>>) o.getJSONObject("plugins").entrySet()) {
                Plugin p = new Plugin(sourceId, e.getValue());
                plugins.put(e.getKey(), p);
            }
        }
    }

    public static class Entry {
        /** {@link UpdateSite} ID. */
        @NonNull public final String sourceId;

        /** Artifact ID. */
        @NonNull public final String name;

        /** The version. */
        @NonNull public final String version;

        /** Download URL. */
        @NonNull public final String url;

        public Entry(String sourceId, JSONObject o) {
            this.sourceId = sourceId;
            this.name = o.getString("name");
            this.version = o.getString("version");
            this.url = o.getString("url");
        }
    }

    public static class Plugin extends Entry {
        /** Human readable title of the plugin. */
        @CheckForNull public final String title;

        /** Dependencies of this plugin, a name -&gt; version mapping. */
        @NonNull public final Map<String, String> dependencies = new HashMap<>();

        /** Optional dependencies of this plugin. */
        @NonNull public final Map<String, String> optionalDependencies = new HashMap<>();

        public Plugin(String sourceId, JSONObject o) {
            super(sourceId, o);
            this.title = o.optString("title", null);
            for (Object jo : o.getJSONArray("dependencies")) {
                JSONObject depObj = (JSONObject) jo;
                if (Boolean.parseBoolean(depObj.getString("optional"))) {
                    optionalDependencies.put(depObj.getString("name"), depObj.getString("version"));
                } else {
                    dependencies.put(depObj.getString("name"), depObj.getString("version"));
                }
            }
        }

        public final String getDisplayName() {
            return title != null ? title : name;
        }
    }
}
