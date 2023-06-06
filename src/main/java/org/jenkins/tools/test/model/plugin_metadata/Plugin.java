package org.jenkins.tools.test.model.plugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.jenkins.tools.test.exception.MetadataExtractionException;

/**
 * Metadata representing a specific plugin for testing.
 */
public class Plugin {
    @NonNull
    private final String pluginId;

    @NonNull
    private final String version;

    @NonNull
    private final String gitUrl;

    @CheckForNull
    private final String tag;

    @NonNull
    private final String module;

    @CheckForNull
    private final String gitHash;

    @CheckForNull
    private final String name;

    private Plugin(Builder builder) {
        this.pluginId = Objects.requireNonNull(builder.pluginId, "pluginId may not be null");
        this.version = Objects.requireNonNull(builder.version, "version may not be null");
        this.gitUrl = Objects.requireNonNull(builder.gitUrl, "gitUrl may not be null");
        this.tag = builder.tag;
        this.module = Objects.requireNonNull(builder.module, "module may not be null");
        this.gitHash = builder.gitHash;
        this.name = builder.name;
    }

    /**
     * The unique plugin ID for this plugin.
     */
    @NonNull
    public String getPluginId() {
        return pluginId;
    }

    /**
     * The Git URL for the source repository that contains this plugin; may be file based for a local checkout.
     */
    @NonNull
    public String getGitUrl() {
        return gitUrl;
    }

    /**
     * The version of the plugin.
     */
    @NonNull
    public String getVersion() {
        return version;
    }

    /**
     * The Git tag for this plugin as reported by Maven; may be {@code null} if Maven is not aware of a tag or for a local checkout.
     * Code should prefer {@link #getGitHash()} if obtaining a commit to checkout.
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
     * The module name for this plugin; the module will always have a name, whether it is part of a multi-module build or not.
     */
    @NonNull
    public String getModule() {
        return module;
    }

    /**
     * The plugin name if known, otherwise the plugin ID.
     */
    @NonNull
    public String getName() {
        return name == null ? pluginId : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Plugin that = (Plugin) o;
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

    public static final class Builder {
        private String pluginId;
        private String version;
        private String gitUrl;
        private String tag;
        private String module;
        private String gitHash;
        private String name;

        public Builder() {}

        public Builder(Plugin from) {
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

        public Plugin build() {
            return new Plugin(this);
        }
    }
}
