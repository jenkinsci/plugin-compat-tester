package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Objects;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * Metadata representing a specific plugin for testing.
 */
public class PluginMetadata {

    private final String pluginId;
    private final String gitUrl;
    private final String modulePath;
    private final String gitCommit;
    private final String name;
    private final String version;

    private PluginMetadata(Builder builder) {
        this.pluginId = Objects.requireNonNull(builder.pluginId, "pluginId may not be null");
        this.gitUrl = Objects.requireNonNull(builder.gitUrl, "gitUrl may not be null");
        this.modulePath = builder.modulePath;
        this.gitCommit = builder.gitCommit;
        this.name = builder.name;
        this.version = Objects.requireNonNull(builder.version, "version may not be null");
    }

    /** The unique plugin ID for this plugin. */
    public String getPluginId() {
        return pluginId;
    }

    /** The Git URL for the source repository that contains this plugin; may be file based for a local checkout. */
    public String getGitUrl() {
        return gitUrl;
    }

    /**
     * The sha or tag of the git commit representing this plugin; may be {@code null} for a local checkout.
     */
    @CheckForNull
    public String getGitCommit() {
        return gitCommit;
    }

    /**
     * The module path of the plugin inside its source repository; will be {@code null} or the
     * empty string ({@code ""}) if the plugin is not part of a multi-module build.
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
        private String gitUrl;
        private String modulePath;
        private String gitCommit;
        private String name;
        private String version;

        public Builder() {}

        public Builder(PluginMetadata from) {
            this.pluginId = from.pluginId;
            this.gitUrl = from.gitUrl;
            this.modulePath = from.modulePath;
            this.gitCommit = from.gitCommit;
            this.name = from.name;
            this.version = from.version;
        }

        public Builder withPluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        /**
         * Convenience method that strips {@code scm:git:} from the URL and sets the Git URL.
         * @param scmUrl the Maven model SCM URL
         * @throws MetadataExtractionException If the underlying SCM is not a Git URL
         * @see #withGitUrl(String)
         */
        public Builder withScmUrl(String scmUrl) throws MetadataExtractionException {
            if (scmUrl.startsWith("scm:git:")) {
                return withGitUrl(scmUrl.substring(8));
            }
            throw new MetadataExtractionException(
                    "SCM URL" + scmUrl + " is not supported, only Git SCM URLs are supported");
        }

        public Builder withGitUrl(String gitUrl) {
            this.gitUrl = gitUrl;
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
