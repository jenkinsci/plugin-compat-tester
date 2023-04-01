package org.jenkins.tools.test.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class UpdateSite {

    /** In-memory representation of the update center data. */
    public static final class Data {
        /** The latest jenkins.war. */
        @NonNull
        public final Entry core;

        /** Plugins in the repository, keyed by their artifact IDs. */
        @NonNull
        public final Map<String, Plugin> plugins = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Data(@NonNull Entry core, @NonNull List<Plugin> plugins) {
            this.core = core;
            for (Plugin plugin : plugins) {
                this.plugins.put(plugin.name, plugin);
            }
        }
    }

    public static class Entry {
        /** Artifact ID. */
        @NonNull
        public final String name;

        /** The version. */
        @NonNull
        public final String version;

        /** Download URL. */
        @NonNull
        public final String url;

        public Entry(@NonNull String name, @NonNull String version, @NonNull String url) {
            this.name = name;
            this.version = version;
            this.url = url;
        }
    }

    public static class Plugin extends Entry {
        /** Human readable title of the plugin. */
        @CheckForNull
        public final String title;

        public Plugin(@NonNull String name, @NonNull String version, @NonNull String url, @CheckForNull String title) {
            super(name, version, url);
            this.title = title;
        }

        public final String getDisplayName() {
            return title != null ? title : name;
        }
    }
}
