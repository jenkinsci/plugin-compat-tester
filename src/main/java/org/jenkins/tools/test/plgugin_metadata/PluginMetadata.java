package org.jenkins.tools.test.plgugin_metadata;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Objects;

public class PluginMetadata {

    private final String pluginId;
    private final String scmUrl;
    private final String modulePath;
    private final String gitCommit;

    private PluginMetadata(Builder builder) {
        this.pluginId = Objects.requireNonNull(builder.pluginId);
        this.scmUrl = Objects.requireNonNull(builder.scmUrl);
        this.modulePath = builder.modulePath;
        this.gitCommit = Objects.requireNonNull(builder.gitCommit);
    }

    /**
     * @return the pluginId
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * @return the git SCM URL
     */
    public String getScmUrl() {
        return scmUrl;
    }

    /**
     * @return the gitCommit
     */
    public String getGitCommit() {
        return gitCommit;
    }

    @CheckForNull
    public String getModulePath() {
        return modulePath;
    }

    public static final class Builder {
        private String pluginId;
        private String scmUrl;
        private String modulePath;
        private String gitCommit;

        public Builder() {}

        public Builder(PluginMetadata from) {
            this.pluginId = from.getPluginId();
            this.scmUrl = from.getScmUrl();
            this.modulePath = from.getModulePath();
            this.gitCommit = from.getGitCommit();
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

        public PluginMetadata build() {
            return new PluginMetadata(this);
        }
    }
}
