Plugin Compatibility Tester (PCT)
------

Generates a compatibility matrix for plugins against Jenkins core.

See https://wiki.jenkins-ci.org/display/JENKINS/Plugin+Compatibility+Tester for background.

## Running PCT 

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
  -war jenkins.war localCheckoutDir ${PLUGIN_SRC} \
  -skipTestCache true \
  -mvn ${PATH_TO_MAVEN}
```

You can run the CLI with the `-help` argument to get a full list of supported options.

:exclamation: For the moment testing more than one plugin at once requires plugins to be released, so for testing SNAPSHOTS you need to execute the last step for every plugin you want to test*

## TODOs

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
