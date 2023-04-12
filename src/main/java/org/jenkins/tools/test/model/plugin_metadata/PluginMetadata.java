package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * Metadata representing a specific plugin for testing.
 */
public class PluginMetadata {
    @NonNull
    private final String pluginId;

    @NonNull
    private final String version;

    @NonNull
    private final String gitUrl;

    private final String tag;
    private final String module;
    private final String gitHash;
    private final String name;

    private PluginMetadata(Builder builder) {
        this.pluginId = Objects.requireNonNull(builder.pluginId, "pluginId may not be null");
        this.version = Objects.requireNonNull(builder.version, "version may not be null");
        this.gitUrl = Objects.requireNonNull(builder.gitUrl, "gitUrl may not be null");
        this.tag = builder.tag;
        this.module = builder.module;
        this.gitHash = builder.gitHash;
        this.name = builder.name;
    }

    /**
     * The unique plugin ID for this plugin.
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * The Git URL for the source repository that contains this plugin; may be file based for a local checkout.
     */
    public String getGitUrl() {
        return gitUrl;
    }

    /**
     * The version of the plugin.
     */
    public String getVersion() {
        return version;
    }

    /**
     * The Git tag for this plugin as reported by Maven; may be {@code null} if Maven is not aware of a tag or for a local checkout.
     */
    @CheckForNull
    public String getTag() {
        return tag;
    }

    /**
     * The Git hash for this plugin; will be {@code null} for a local checkout.
     */
    @CheckForNull
    public String getGitHash() {
        return gitHash;
    }

    /**
     * The module name for this plugin; will be {@code null} if the plugin is not part of a multi-module build.
     */
    @CheckForNull
    public String getModule() {
        return module;
    }

    /**
     * The plugin name if known, otherwise the plugin ID.
     */
    public String getName() {
        return name == null ? pluginId : name;
    }

    public static final class Builder {
        private String pluginId;
        private String version;
        private String gitUrl;
        private String tag;
        private String module;
        private String gitHash;
        private String name;

        public Builder() {}

        public Builder(PluginMetadata from) {
            this.pluginId = from.pluginId;
            this.version = from.version;
            this.gitUrl = from.gitUrl;
            this.tag = from.tag;
            this.module = from.module;
            this.gitHash = from.gitHash;
            this.name = from.name;
        }

        public Builder withPluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        /**
         * Convenience method that strips {@code scm:git:} from the URL and sets the Git URL.
         * @param scmUrl the Maven model SCM URL
         * @throws MetadataExtractionException If the underlying SCM is not a Git URL
         * @see #withGitUrl(String)
         */
        public Builder withScmConnection(String scmUrl) throws MetadataExtractionException {
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

        public Builder withTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder withModule(String module) {
            this.module = module;
            return this;
        }

        public Builder withGitHash(String gitHash) {
            this.gitHash = gitHash;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public PluginMetadata build() {
            return new PluginMetadata(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginMetadata that = (PluginMetadata) o;
        return getPluginId().equals(that.getPluginId())
                && getVersion().equals(that.getVersion())
                && getGitUrl().equals(that.getGitUrl())
                && Objects.equals(getTag(), that.getTag())
                && Objects.equals(getModule(), that.getModule())
                && Objects.equals(getGitHash(), that.getGitHash())
                && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPluginId(), getVersion(), getGitUrl(), getTag(), getModule(), getGitHash(), getName());
    }
}
