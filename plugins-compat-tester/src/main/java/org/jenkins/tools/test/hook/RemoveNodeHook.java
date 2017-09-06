package org.jenkins.tools.test.hook;

import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.exception.PomExecutionException;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This hook removes the node and node_modules folders from BO before compiling it
 */
public class RemoveNodeHook extends PluginCompatTesterHookBeforeCompile {

    public RemoveNodeHook() {
        System.out.println("Loaded RemoveNodeHook");
    }

    @Override
    public List<String> transformedPlugins() {
        List<String> transformedPlugins = new LinkedList<>();
        transformedPlugins.addAll(BlueOceanHook.BO_PLUGINS);
        return transformedPlugins;
    }

    @Override
    public void validate(Map<String, Object> toCheck) throws Exception {
        //Empty by design
    }

    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        try {
            System.out.println("Executing RemoveNodeHook hook");

            File pluginDir = (File) moreInfo.get("pluginDir");
            removeNodeFolders(pluginDir);
            System.out.println("Executed RemoveNodeHook hook");
            return moreInfo;
            // Exceptions get swallowed, so we print to console here and rethrow again
        } catch (Exception e) {
            System.out.println("Exception executing hook");
            System.out.println(e);
            throw e;
        }
    }

    private void removeNodeFolders(File path) throws PomExecutionException, IOException {
        File nodeFolder = new File(path, "node");
        if (nodeFolder.exists() && nodeFolder.isDirectory()) {
           FileUtils.deleteDirectory(nodeFolder);
        }
        File nodeModulesFolder = new File(path, "node_modules");
        if (nodeModulesFolder.exists() && nodeModulesFolder.isDirectory()) {
            FileUtils.deleteDirectory(nodeModulesFolder);
        }
    }
}