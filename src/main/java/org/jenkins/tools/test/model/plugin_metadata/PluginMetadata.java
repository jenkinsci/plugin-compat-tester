package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Objects;

public class PluginMetadata {

    private final String pluginId;
    private final String scmUrl;
    private final String modulePath;
    private final String gitCommit;
    private final String name;
    private final String version;

    private PluginMetadata(Builder builder) {
        this.pluginId = Objects.requireNonNull(builder.pluginId);
        this.scmUrl = Objects.requireNonNull(builder.scmUrl);
        this.modulePath = builder.modulePath;
        this.gitCommit = builder.gitCommit;
        this.name = builder.name;
        this.version = Objects.requireNonNull(builder.version);
    }

    /** The unique plugin id for this plugin. */
    public String getPluginId() {
        return pluginId;
    }

    /** The git URL for the source repository that contains this plugin. */
    public String getScmUrl() {
        return scmUrl;
    }

    /**
     * The sha of the git commit representing this plugin, may be {@code null} for a local checkout.
     */
    @CheckForNull
    public String getGitCommit() {
        return gitCommit;
    }

    /**
     * The module path of the plugin inside it's source repository, will be {@code null} or the
     * empty string ({@code ""}) if the plugin is not part of a multi module build.
     */
    @CheckForNull
    public String getModulePath() {
        return modulePath;
    }

    /** The plugin name if known, otherwise the plugin id. */
    public String getName() {
        return name == null ? pluginId : name;
    }

    /** The version of the plugin. */
    public String getVersion() {
        return version;
    }

    public static final class Builder {
        private String pluginId;
        private String scmUrl;
        private String modulePath;
        private String gitCommit;
        private String name;
        private String version;

        public Builder() {}

        public Builder(PluginMetadata from) {
            this.pluginId = from.pluginId;
            this.scmUrl = from.scmUrl;
            this.modulePath = from.modulePath;
            this.gitCommit = from.gitCommit;
            this.name = from.name;
            this.version = from.version;
        }

        public Builder withPluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        public Builder withScmUrl(String scmUrl) {
            this.scmUrl = scmUrl;
            return this;
        }

        public Builder withModulePath(String modulePath) {
            this.modulePath = modulePath;
            return this;
        }

        public Builder withGitCommit(String gitCommit) {
            this.gitCommit = gitCommit;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public PluginMetadata build() {
            return new PluginMetadata(this);
        }
    }
}
