package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

/**
 * Utility class to ease create simple hooks for multi-module projects
 */
public abstract class AbstractMultiParentHook extends PluginCompatTesterHookBeforeCheckout {

    private static final Logger LOGGER = Logger.getLogger(AbstractMultiParentHook.class.getName());

    protected boolean firstRun = true;
    
    private PomData pomData;

    @Override
    public Map<String, Object> action(Map<String, Object> moreInfo) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig)moreInfo.get("config");
        UpdateSite.Plugin currentPlugin = (UpdateSite.Plugin)moreInfo.get("plugin");


        // We should not execute the hook if using localCheckoutDir
        boolean shouldExecuteHook = config.getLocalCheckoutDir() == null || !config.getLocalCheckoutDir().exists();

        if (shouldExecuteHook) {
            LOGGER.log(Level.INFO, "Executing hook for {0}", getParentProjectName());
            // Determine if we need to run the download; only run for first identified plugin in the series
            if (firstRun) {
                LOGGER.log(Level.INFO, "Preparing for multi-module checkout");

                // Checkout to the parent directory. All other processes will be on the child directory
                File parentPath = new File(config.workDirectory.getAbsolutePath() + "/" + getParentFolder());

                pomData = (PomData) moreInfo.get("pomData");
                String scmTag;
                if (pomData.getScmTag() != null) {
                    scmTag = pomData.getScmTag();
                    LOGGER.log(Level.INFO, "Using SCM tag {0} from POM", scmTag);
                } else {
                    scmTag = getParentProjectName() + "-" + currentPlugin.version;
                    LOGGER.log(Level.INFO, "POM did not provide an SCM tag; inferring tag {0}", scmTag);
                }
                // Like PluginCompatTester.cloneFromSCM but with subdirectories trimmed:
                cloneFromSCM(currentPlugin, parentPath, scmTag, getUrl(), config.getFallbackGitHubOrganization());
            }

            // Checkout already happened, don't run through again
            moreInfo.put("runCheckout", false);
            firstRun = false;

            // Change the "download"" directory; after download, it's simply used for reference
            File childPath = new File(config.workDirectory.getAbsolutePath() + "/" + getParentFolder() + "/" + getPluginFolderName(currentPlugin));

            LOGGER.log(Level.INFO, "Child path for {0}: {1}", new Object[]{currentPlugin.getDisplayName(), childPath.getPath()});
            moreInfo.put("checkoutDir", childPath);
            moreInfo.put("pluginDir", childPath);
            moreInfo.put("parentFolder", getParentFolder());
        } else {
            configureLocalCheckOut(currentPlugin, config.getLocalCheckoutDir(), moreInfo);
        }

        return moreInfo;
    }

    private void cloneFromSCM(UpdateSite.Plugin currentPlugin, File parentPath, String scmTag, String url, String fallbackGitHubOrganization)
            throws ComponentLookupException, ScmRepositoryException, NoSuchScmProviderException, ScmException, IOException {
        
        List<String> connectionURLs = new ArrayList<String>();
        connectionURLs.add(url);
        if(fallbackGitHubOrganization != null){
            connectionURLs = PluginCompatTester.getFallbackConnectionURL(connectionURLs, url, fallbackGitHubOrganization);
        }
        
        Boolean repositoryCloned = false;
        String errorMessage = "";
        ScmRepository repository;
        ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
        for (String connectionURL: connectionURLs){
            if (connectionURL != null) {
                connectionURL = connectionURL.replace("git://", "https://"); // See: https://github.blog/2021-09-01-improving-git-protocol-security-github/
            }
            LOGGER.log(Level.INFO, "Checking out from SCM connection URL {0}: {1} ({2}-{3}) at tag {4}", new Object[]{connectionURL, getParentProjectName(), currentPlugin.version, scmTag});
            if (parentPath.isDirectory()) {
                FileUtils.deleteDirectory(parentPath);
            }
            repository = scmManager.makeScmRepository(connectionURL);
            CheckOutScmResult result = scmManager.checkOut(repository, new ScmFileSet(parentPath), new ScmTag(scmTag));
            if(result.isSuccess()){
                repositoryCloned = true;
                break;
            } else {
                errorMessage = result.getProviderMessage() + " || " + result.getCommandOutput();
            }
        }
        
        if (!repositoryCloned) {
            // Throw an exception if there are any download errors.
            throw new RuntimeException(errorMessage);
        }
    }

    public String getUrl() {
        return pomData.getConnectionUrl().replaceFirst("^(.+github[.]com/[^/]+/[^/]+)/.+", "$1");
    }

    protected void configureLocalCheckOut(UpdateSite.Plugin currentPlugin, File localCheckoutDir, Map<String, Object> moreInfo) {
        // Do nothing to keep compatibility with pre-existing Hooks
        LOGGER.log(Level.INFO, "Ignoring local checkout directory for {0}", currentPlugin.getDisplayName());
    }

    /**
     * Returns the folder where the multi-module project parent will be checked out
     */
    protected abstract String getParentFolder();

    /**
     * Returns the parent project name. This will be used to form the checkout tag with the format
     * {@code parentProjectName-version}.
     */
    protected abstract String getParentProjectName();

    /**
     * Returns the plugin folder name. By default it will be the plugin name, but it can be
     * overridden to support plugins (like {@code structs}) that are not located in a folder with
     * the same name as the plugin itself.
     */
    protected String getPluginFolderName(UpdateSite.Plugin currentPlugin) {
        return currentPlugin.name;
    }
}
