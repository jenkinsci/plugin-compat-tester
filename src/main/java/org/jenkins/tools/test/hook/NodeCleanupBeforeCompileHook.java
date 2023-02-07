package org.jenkins.tools.test.hook;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

public class NodeCleanupBeforeCompileHook extends PluginCompatTesterHookBeforeCompile {

    private static final Logger LOGGER =
            Logger.getLogger(NodeCleanupBeforeCompileHook.class.getName());

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
        boolean shouldExecuteHook =
                config.getIncludePlugins().contains("sse-gateway")
                        || config.getIncludePlugins().contains("workflow-cps");

        if (shouldExecuteHook) {
            File pluginDir = (File) moreInfo.get("pluginDir");
            LOGGER.log(Level.INFO, "Executing node and node_modules cleanup hook");
            compile(pluginDir);
            return moreInfo;
        } else {
            LOGGER.log(Level.INFO, "Hook not triggered; continuing");
            return moreInfo;
        }
    }

    @Override
    public void validate(Map<String, Object> toCheck) {}

    private void compile(File path) {
        LOGGER.log(Level.INFO, "Calling removeNodeFolders");
        removeNodeFolders(path);
    }

    private void removeNodeFolders(File path) {
        File nodeFolder = new File(path, "node");
        if (nodeFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(nodeFolder);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        File nodeModulesFolder = new File(path, "node_modules");
        if (nodeModulesFolder.isDirectory()) {
            try {
                FileUtils.deleteDirectory(nodeFolder);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
