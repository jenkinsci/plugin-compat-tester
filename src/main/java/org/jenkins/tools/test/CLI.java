package org.jenkins.tools.test;

import picocli.CommandLine;

@CommandLine.Command(
        name = "pct",
        mixinStandardHelpOptions = true,
        subcommands = {PluginCompatTesterCli.class, PluginListerCLI.class},
        versionProvider = VersionProvider.class)
public class CLI {

    public static void main(String... args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}
