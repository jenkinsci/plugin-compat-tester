package org.jenkins.tools.test.model.hook;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.plugin_metadata.Plugin;

public final class BeforeExecutionContext extends StageContext {

    @NonNull
    private final File cloneDirectory;

    @NonNull
    private final List<String> args;

    @NonNull
    private final NavigableMap<String, String> overrideVersions = new TreeMap<>();

    @NonNull
    private final NavigableSet<String> upperBoundsExcludes = new TreeSet<>();

    public BeforeExecutionContext(
            @NonNull String coreVersion,
            @NonNull Plugin plugin,
            @NonNull PluginCompatTesterConfig config,
            @NonNull File cloneDirectory,
            @NonNull List<String> args) {
        super(Stage.EXECUTION, coreVersion, plugin, config);
        this.cloneDirectory = cloneDirectory;
        this.args = args;
    }

    @NonNull
    public File getCloneDirectory() {
        return cloneDirectory;
    }

    @NonNull
    public List<String> getArgs() {
        return args;
    }

    /**
     * Map of dependency version overrides in the form {@code groupId:artifactId} to {@code version} to apply during
     * testing.
     */
    @NonNull
    public NavigableMap<String, String> getOverrideVersions() {
        return overrideVersions;
    }

    /**
     * Set of exclusions to upper bound updates in the form {@code groupId:artifactId}.
     */
    @NonNull
    public NavigableSet<String> getUpperBoundsExcludes() {
        return upperBoundsExcludes;
    }
}
