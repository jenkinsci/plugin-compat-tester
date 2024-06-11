Plugin Compatibility Tester (PCT)
------

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/jenkinsci/plugin-compat-tester?label=changelog)](https://github.com/jenkinsci/plugin-compat-tester/releases)

Generate a compatibility matrix for plugins against Jenkins core.

## Running PCT

The PCT CLI requires passing a WAR file containing plugins (generated from `jenkinsci/bom`, for example) as input;
the versions of the plugins are inferred from the WAR file contents.

```shell
java -jar target/plugins-compat-tester-cli.jar \
    --war "$(pwd)/megawar.war" \
    --working-dir "$(pwd)/pct-work"
```

To test a subset of plugins in the WAR, use `--include-plugins`:

```shell
java -jar target/plugins-compat-tester-cli.jar \
    --war "$(pwd)/megawar.war" \
    --include-plugins ssh-slaves,credentials \
    --working-dir "$(pwd)/pct-work"
```

You can run the CLI with the `--help` argument to get a full list of supported options.

### Running PCT with custom Java versions

PCT simply invokes Maven, which relies on the `JAVA_HOME` environment variable.
If you want to use a custom Java version, set `JAVA_HOME` appropriately before running PCT.

## Useful links

* Devoxx '11 BOF Presentation about Plugin Compat Tester is available [here](http://www.slideshare.net/fcamblor/devoxx-2011-jenkins-bof-on-plugin-compatibility-tester). This presentation is partially obsolete (GAE feature was removed in recent versions)
