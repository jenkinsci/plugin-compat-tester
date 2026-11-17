Plugin Compatibility Tester (PCT)
------

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/jenkinsci/plugin-compat-tester?label=changelog)](https://github.com/jenkinsci/plugin-compat-tester/releases)

Generates a compatibility matrix for plugins against Jenkins core.

## How PCT works

By default, PCT runs a plugin's tests against the Jenkins core in the supplied WAR:

1. Read the Jenkins version from the WAR manifest and select the bundled plugins to test.
2. Read each plugin's metadata to find its source repository and revision, then check out the sources under the working directory.
   Plugins from the same repository share a checkout.
3. Compile the plugin and its tests using the original POM with `clean process-test-classes`.
   The plugin is compiled against its original Jenkins version before its tests run against the target version.
4. Resolve the test dependencies and run the compiled tests against the supplied WAR.
   PCT hooks can adjust the build for individual plugins before checkout, compilation, or test execution.

With `--local-checkout-dir`, PCT uses the supplied sources instead of checking out the plugins bundled in the WAR.
With `--compile-only`, it checks compilation against the target Jenkins version instead of running the tests.

### Checking the results

Maven output is printed to the console and saved under `<working-dir>/logs/`.
For test runs, each plugin's log is stored at
`logs/<plugin-id>/v<plugin-version>_against_core_version_<jenkins-version>.log`.
The plugin's Maven Surefire reports are available in its checkout, normally under `target/surefire-reports/`
(or the plugin module's `target/surefire-reports/` for a multi-module project).

PCT stops at the first failure by default.
Use `--no-fail-fast` to continue testing the remaining plugins; the overall run still fails if any plugin failed.

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
