package org.jenkins.tools.test.hook;

import hudson.model.UpdateSite;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.jenkins.tools.test.SCMManagerFactory;
import org.jenkins.tools.test.PluginCompatTester;
import org.jenkins.tools.test.model.PluginCompatTesterConfig;
import org.jenkins.tools.test.model.PomData;
import org.jenkins.tools.test.model.comparators.VersionComparator;
import org.jenkins.tools.test.model.hook.PluginCompatTesterHookBeforeCheckout;

/**
 * This hook allows plugins using a non-standard tag format on GitHub to be checked out.
 *
 * <p>The affected plugins are loaded from a properties file whose key is the {@code artifactId} and
 * whose value is a string to be formatted for the tag value. This value will be calculated using
 * {@link String#format(String, Object...)} passing the current version as the only parameter.
 */
public class NonStandardTagHook  extends PluginCompatTesterHookBeforeCheckout {

    private final Properties affectedPlugins;
    private final List<String> transformedPlugins = new LinkedList<>();
    private final VersionComparator comparator = new VersionComparator();
    private static final String MINIMUM_VERSION_SUFFIX = "-minimumVersion";

    public NonStandardTagHook()  {
        affectedPlugins = new Properties();
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("nonstandardtagplugins.properties")) {
            affectedPlugins.load(inputStream);
            affectedPlugins.keySet().forEach(e -> {
                if (!e.toString().contains(MINIMUM_VERSION_SUFFIX)) {
                    transformedPlugins.add(e.toString());
                }
            });
        } catch (IOException e) {
            System.err.println("WARNING: NonStandardTagHook was not able to load affected plugins, the hook will do nothing");
            e.printStackTrace();
        }
    }

    @Override
    public boolean check(Map<String, Object> info) {
        UpdateSite.Plugin plugin = (UpdateSite.Plugin) info.get("plugin");
        boolean definedPlugin = affectedPlugins.containsKey(plugin.name);
        String minimumVersion= affectedPlugins.getProperty(plugin.name + MINIMUM_VERSION_SUFFIX);
        boolean correctVersion = minimumVersion == null || comparator.compare(minimumVersion, plugin.version) <= 0;
        return definedPlugin && correctVersion;
    }

    @Override
    public Map<String, Object> action(Map<String, Object> info) throws Exception {
        PluginCompatTesterConfig config = (PluginCompatTesterConfig)info.get("config");
        PomData pomData = (PomData)info.get("pomData");
        UpdateSite.Plugin plugin = (UpdateSite.Plugin) info.get("plugin");

        // We should not execute the hook if using localCheckoutDir
        boolean shouldExecuteHook = config.getLocalCheckoutDir() == null || !config.getLocalCheckoutDir().exists();

        if (shouldExecuteHook) {
            System.out.println("Executing " + this.getClass().getSimpleName() + " for " + pomData.artifactId);

            // Checkout to the parent directory. All other processes will be on the child directory
            File checkoutPath = new File(config.workDirectory.getAbsolutePath() + "/" + pomData.artifactId);

            String scmTag =  String.format(affectedPlugins.get(pomData.artifactId).toString(), plugin.version);
            PluginCompatTester pct = new PluginCompatTester(config);
            pct.cloneFromSCM(pomData, plugin.name, plugin.version, checkoutPath, scmTag, info);

            // Checkout already happened, don't run through again
            info.put("runCheckout", false);
            info.put("checkoutDir", checkoutPath);
            info.put("pluginDir", checkoutPath);
        }
        return info;

    }

    @Override
    public List<String> transformedPlugins() {
        return Collections.unmodifiableList(transformedPlugins);
    }
}
