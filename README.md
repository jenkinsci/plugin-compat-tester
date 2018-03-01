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
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -e ARTIFACT_ID=ssh-slaves -e VERSION=ssh-slaves-1.24 jenkins/pct
```

This command will run PCT for the latest plugin version against the specified Jenkins WAR.
PCT supports running against SNAPSHOT builds, but PCT will have to install local Maven artifacts in such case.

```shell
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -v my/jenkins.war:/pct/jenkins.war:ro -e ARTIFACT_ID=ssh-slaves jenkins/pct
```

This command will run PCT against custom versions of Jenkins and the plugin specified by volumes.

```shell
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -v my/jenkins.war:/pct/jenkins.war:ro -v my/plugin:/pct/plugin-src:ro jenkins/pct
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

Volumes:

* `/pct/plugin-src` - Plugin sources to be used for the PCT run. Sources will be checked out if not specified
* `/pct/jenkins.war` - Jenkins WAR file to be used for the PCT run
* `/pct/m2-settings.xml` - Custom Maven Settings file (optional)
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
  -mvn ${PATH_TO_MAVEN}
```

You can run the CLI with the `-help` argument to get a full list of supported options.

:exclamation: For the moment testing more than one plugin at once requires plugins to be released, so for testing SNAPSHOTS you need to execute the last step for every plugin you want to test*

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
