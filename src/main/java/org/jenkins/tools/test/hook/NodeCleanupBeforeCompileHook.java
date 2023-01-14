package org.jenkins.tools.test.hook;

import org.apache.commons.io.FileUtils;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCompile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NodeCleanupBeforeCompileHook extends PluginCompatTesterHookBeforeCompile {

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig) moreInfo.get("config");
        boolean shouldExecuteHook = config.getIncludePlugins().contains("sse-gateway") || config.getIncludePlugins().contains("workflow-cps");

        if (shouldExecuteHook) {
            File pluginDir = (File) moreInfo.get("pluginDir");
            try {
                System.out.println("Executing node and node_modules cleanup hook");
                compile(pluginDir);
                return moreInfo;
            } catch (Exception e) {
                System.out.println("Exception executing hook");
                System.out.println(e);
                throw e;
            }
        } else {
            System.out.println("Hook not triggered. Continuing.");
            return moreInfo;
        }
    }

    @Override
    public void validate(Map<String, Object> toCheck) {
    }

    private void compile(File path) throws IOException {
        System.out.println("Calling removeNodeFolders");
        removeNodeFolders(path);
    }

    private void removeNodeFolders(File path) throws IOException {
        File nodeFolder = new File(path, "node");
        if (nodeFolder.isDirectory()) {
            FileUtils.deleteDirectory(nodeFolder);
        }
        File nodeModulesFolder = new File(path, "node_modules");
        if (nodeModulesFolder.isDirectory()) {
            FileUtils.deleteDirectory(nodeModulesFolder);
        }
    }

}
