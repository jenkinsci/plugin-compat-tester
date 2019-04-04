Plugin Compatibility Tester (PCT)
------

Generates a compatibility matrix for plugins against Jenkins core.

See https://wiki.jenkins-ci.org/display/JENKINS/Plugin+Compatibility+Tester for background.

## Running PCT 

### Running PCT in Docker

It is recommended to run PCT in the prepared Docker image.
You can find it [here](https://hub.docker.com/r/jenkins/pct/).

#### Examples

This command will clone of a repository and run PCT against the latest Jenkins Core:

```shell
docker run -ti --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -e ARTIFACT_ID=ssh-slaves -e VERSION=ssh-slaves-1.24 jenkins/pct
```

This command will run PCT for the latest plugin version against the specified Jenkins WAR.
PCT supports running against SNAPSHOT builds, but PCT will have to install local Maven artifacts in such case.

```shell
docker run -ti --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -v my/jenkins.war:/pct/jenkins.war:ro -e ARTIFACT_ID=ssh-slaves jenkins/pct
```

This command will run PCT against custom versions of Jenkins and the plugin specified by volumes.

```shell
docker run -ti --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -v my/jenkins.war:/pct/jenkins.war:ro -v my/plugin:/pct/plugin-src:ro jenkins/pct
```

The command below will run PCT for a branch in a custom repository.

```shell
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -e CHECKOUT_SRC=https://github.com/oleg-nenashev/job-restrictions-plugin.git -e VERSION=JENKINS-26374 jenkins/pct
```

#### Configuration

Environment variables:

* `ARTIFACT_ID` - ID of the artifact to be tested.
The image will be able to determine this ID automatically if `CHECKOUT_SRC` or `/pct/plugin-src` are defined.
* `VERSION` - tag/commit/branch to be checked out and tested. `master` by default
* `CHECKOUT_SRC` - Custom Git clone source (e.g. `https://github.com/oleg-nenashev/job-restrictions-plugin.git`). `https://github.com/jenkinsci/${ARTIFACT_ID}-plugin.git` by default
* `JAVA_OPTS` - Java options to be passed to the PCT CLI
* `DEBUG` - Boolean flag, which enables the Remote Debug mode (port == 5000)
* `M2_SETTINGS_FILE` -  If set indicates the path of the custom maven settings file to use (see volumes below)
* `INSTALL_BUNDLED_SNAPSHOTS` - Install JAR and plugin snapshots to local repository.
        `true` by default. WAR snapshots will be always installed.

Volumes:

* `/pct/plugin-src` - Plugin sources to be used for the PCT run. Sources will be checked out if not specified
* `/pct/jenkins.war` - Jenkins WAR file to be used for the PCT run
* `/pct/m2-settings.xml` - Custom Maven Settings (optional) if `M2_SETTINGS_FILE` environment variable exists `run-pct` will ignore this location and use the one specified in the variable
* `/pct/out` - Output directory for PCT. All reports will be stored there
* `/pct/tmp` - Temporary directory. Can be exposed to analyze run failures
* `/root/.m2` - Maven repository. It can be used to pass settings.xml or to cache artifacts

### Running PCT manually

PCT offers the CLI interface which can be used to run PCT locally.

* Download `PCT` and execute `mvn clean install`
* Download plugin sources to a `PLUGIN_SRC` directory. 
* Checkout the plugin repo to a tag/commit/branch you want to test
* Run `mvn clean install -DskipTests` to initialize artifacts
* Go to `PCT` folder and run the CLI (make sure to modify paths according to your system). Example:

```shell
java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli-${PCT_VERSION}.jar \
  -reportFile $(pwd)/out/report.xml \
  -workDirectory $(pwd)/tmp/work \
  -includePlugins ${PLUGIN_ARTIFACT_ID} \
  -war jenkins.war -localCheckoutDir ${PLUGIN_SRC} \
  -skipTestCache true \
  -failOnError \
  -mvn ${PATH_TO_MAVEN}
```

You can run the CLI with the `-help` argument to get a full list of supported options.

:exclamation: For the moment testing more than one plugin at once requires plugins to be released, so for testing SNAPSHOTS you need to execute the last step for every plugin you want to test*

### Running PCT with custom Java versions

Plugin compat tester supports running Test Suites with a Java version different 
from the one being used to run PCT and build the plugins.
For example, such mode can be used to run tests with JDK11 starting from Jenkins 2.163.

Two options can be passed to PCT CLI for such purpose:

* `testJDKHome` - A path to JDK HOME to be used for running tests in plugins
* `testJavaArgs` - Java test arguments to be used for test runs.
                   This option may be used to pass custom classpath and, in Java 11, additional modules

You can run the example by running the following command:

    make demo-jdk11 -e TEST_JDK_HOME=${YOUR_JDK11_HOME} -e PLUGIN_NAME=git
    
When using the Docker image, it is possible to use `JDK_VERSION` variable to select 
the version to use. The version needs to be bundled in the docker image.
Currently Java 8 and Java 11 are bundled (JDK_VERSION= {8, 11}).
    
    make demo-jdk11-docker -e JDK_VERSION=11 PLUGIN_NAME=git

Full list of options for JDK11 can be found [here](./Makefile).

### Running PCT with different version of dependencies

IMPORTANT: At this moment, the replacement of a dependency will not update the transitive dependencies.
Because of this, you might have to provide on the same parameter the new versions of the transitive dependencies.

Plugin Compat Tester supports overriding the plugin dependency version. 
For example, we might want to validate that a newer version of a plugin is not breaking the latest version of the plugin we want to test.

To do that, the option `overridenPlugins` can be passed to PCT CLI.
The format of the value **must** be `PLUGIN_NAME=PLUGIN_VERSION`.

So, running

```
java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar \
  [...]
  -overridenPlugins display-url-api=2.3.0
  -includePlugins mailer
```

will run the PCT on the `mailer` plugin, but replacing the `display-url-api` dependency of Mailer (which is `1.0`) with the version `2.3.0`.

## Developer Info

### Debugging PCT in Docker

If you need to debug a dockerized PCT instance, 
use the `-e DEBUG=true -p 5005:5005` flags and then attach to the container using Remote Debugger in your IDE.

```
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -e ARTIFACT_ID=job-restrictions -e DEBUG=true -p 5005:5005 -i jenkins/pct
```

### TODOs

To do (refile in `plugin-compat-tester` in JIRA!):

1. `InternalMavenRunner` currently still seems to run `install` goal which is very undesirable on release tags
1. should run `surefire-report:report` goal instead (or `surefire-report:report-only` after) and display link to HTML results from index page
1. Export *everything* to GAE, dropping the data storing in XML files (which pollutes the filesystem and can be easily delete if we are careless) and processing with XSL. (migration already started to GAE datastorage, but not completely finished, especially on build logs). (jglick: this is undesirable, need to be able to review local results without uploading them)
1. Improve GAE app to allow plugin maintainers to subscribe to notifications on plugin compatibility tests for their plugins against new jenkins versions released.
1. Remove possibility, on GAE app, to select both "every plugins" and "every cores" results... because it generates too much results and crash GAE datastore
1. most plugin tests fail to build using internal Maven; `PlexusWagonProvider.lookup` with a `roleHint=https` fails for no clear reason, and some missing `SNAPSHOT`s cause a build failure related to https://github.com/stapler/stapler-adjunct-codemirror/commit/da995b03a1f165fef7c9d34eadb15797f58399cd
1. testing a module not at the root of its Git repo fails (`findbugs` succeeds but tests against old Jenkins core)
1. testing `analysis-core` fails because it uses `org.jvnet.hudson.plugins:analysis-pom` as a parent
1. when testing a plugin depending on other plugins, bump up the dependency to the latest released version…or even build the dependency from `master` sources
