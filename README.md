Plugin Compatibility Tester (PCT)
------

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/jenkinsci/plugin-compat-tester?label=changelog)](https://github.com/jenkinsci/plugin-compat-tester/releases)

Generates a compatibility matrix for plugins against Jenkins core.

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

## Support for Gradle Build Tool (experimental)

Gradle support in PCT is experimental. It aims to let you compile and test Jenkins plugins built with Gradle against a target Jenkins core.

### Requirements

- Your plugin must be a Jenkins plugin built with Gradle and the Jenkins Gradle tooling:
    - [gradle-jpi-plugin](https://github.com/jenkinsci/gradle-jpi-plugin)
    - [Gradle Convention Plugin (recommended)](https://github.com/jenkinsci/gradle-convention-plugin)
- PCT uses Gradle from the system PATH by default.  
  To use a specific Gradle installation, pass: `--gradle /path/to/gradle`

PCT detects Gradle projects by the presence of `build.gradle(.kts)` and/or `settings.gradle(.kts)`.

For local checkouts, PCT needs the generated POM at: `build/publications/mavenJpi/pom-default.xml`
If it is missing, PCT will try to run `generatePomFileForMavenJpiPublication` automatically.

### CLI options relevant to Gradle

- `--gradle` Path to Gradle executable (falls back to `gradle` on PATH).
- `--gradle-properties` Path to a `gradle.properties` file to import.
- `--gradle-args` Comma-separated list of Gradle CLI arguments (e.g., `--info`).
- `--gradle-tasks` Comma-separated list of tasks to run for testing (default: `test,assemble`).
- `-P/--gradle-property` One or more Gradle project properties to pass through (`-Pkey=value`).

PCT also supports these flags with Gradle:
- `--include-plugins` / `--exclude-plugins` to narrow which plugins from the WAR are tested.
- `--local-checkout-dir` to test a local clone

## Examples

Run against the plugins bundled inside a Jenkins WAR file
```bash
java -jar target/plugins-compat-tester-cli.jar test-plugins \
    --war target/megawar.war \
    --working-dir "$(pwd)/pct-work"
```

Local checkout
```bash
java -jar target/plugins-compat-tester-cli.jar test-plugins \
    --war "$(pwd)/megawar.war" \
    --working-dir "$(pwd)/pct-work" \
    --local-checkout-dir example-plugin/
    --gradle-args "--stacktrace,--info"
```

## Useful links

* Devoxx '11 BOF Presentation about Plugin Compat Tester is available [here](http://www.slideshare.net/fcamblor/devoxx-2011-jenkins-bof-on-plugin-compatibility-tester). This presentation is partially obsolete (GAE feature was removed in recent versions)
