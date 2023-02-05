Plugin Compatibility Tester (PCT)
------

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/jenkinsci/plugin-compat-tester?label=changelog)](https://github.com/jenkinsci/plugin-compat-tester/releases)

Generates a compatibility matrix for plugins against Jenkins core.

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

#### Full PCT runs using a war file as input with Docker

The PCT cli supports to pass a war file containing plugins (generated with Custom war Packager for example) as input, in this case the version of
the plugins is infered from the war file contents.

In such scenarios is tipical to want to run the PCT for all plugins contained in the war file, to avoid having to spawn a new docker container for each plugin
you can use the env variables `DO_NOT_OVERRIDE_PCT_CHECKOUT=true` and `FAIL_ON_ERROR=false` to let the PCT CLI (instead of the `run-pct` script) checkout the proper
versions of the plugins and make sure the PCT does not stop before testing all plugins.

By using those env variables you can pass a comma separated list of plugins ids using the `ARTIFACT_ID` env variable

```shell
docker run -ti --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -v my/jenkins.war:/pct/jenkins.war:ro -e ARTIFACT_ID=ssh-slaves,credentials -e DO_NOT_OVERRIDE_PCT_CHECKOUT=true -e FAIL_ON_ERROR=false jenkins/pct
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
        `true` by default.
* `SKIP_LOCAL_SNAPSHOT_INSTALLATION` - If exists WAR snapshots (core and plugins) will not be installed, if not present war snapshots will be installed

Volumes:

* `/pct/plugin-src` - Plugin sources to be used for the PCT run. Sources will be checked out if not specified
* `/pct/jenkins.war` - Jenkins WAR file to be used for the PCT run
* `/pct/m2-settings.xml` - Custom Maven Settings (optional) if `M2_SETTINGS_FILE` environment variable exists `run-pct` will ignore this location and use the one specified in the variable
* `/pct/out` - Output directory for PCT. All reports will be stored there
* `/pct/tmp` - Temporary directory. Can be exposed to analyze run failures
* `/root/.m2` - Maven repository. It can be used to pass settings.xml or to cache artifacts

:exclamation: Note that the entrypoint script of the PCT docker image tries to checkout the plugin sources *before* invoking the PCT if no war is provided. That means for plugins relying on `PreCheckoutHooks` (like multimodule ones) the standard docker run will fail to get sources as the `PreCheckoutHooks` are not run by the docker image entrypoint script. In that case possible workarounds are:

* Using a war file
* Download the sources of the plugin and use the `/pct/plugin-src` volume to inform the PCT about them
* Use the docker image but override the entry point and run manually the PCT inside the image
* Run the PCT manually as explained below

### Running PCT manually

PCT offers the CLI interface which can be used to run PCT locally.

* Download `PCT` and execute `mvn clean install`
* Download plugin sources to a `PLUGIN_SRC` directory. 
* Checkout the plugin repo to a tag/commit/branch you want to test
* Run `mvn clean install -DskipTests` to initialize artifacts
* Go to `PCT` folder and run the CLI (make sure to modify paths according to your system). Example:

```shell
java -jar target/plugins-compat-tester-cli.jar \
  -workDirectory $(pwd)/tmp/work \
  -includePlugins ${PLUGIN_ARTIFACT_ID} \
  -war jenkins.war -localCheckoutDir ${PLUGIN_SRC} \
  -mvn ${PATH_TO_MAVEN}
```

You can run the CLI with the `-help` argument to get a full list of supported options.

:exclamation: For the moment testing more than one plugin at once requires plugins to be released, so for testing SNAPSHOTS you need to execute the last step for every plugin you want to test*

### Running PCT with a BOM file

Plugin Compat Tester supports running test suites using a BOM file as source of truth as follows:

```shell
java -jar target/plugins-compat-tester-cli.jar \
  -workDirectory $(pwd)/tmp/work \
  -includePlugins ${PLUGIN_ARTIFACT_ID} \
  -bom ${BOM_FILE_LOCATION} \
  -mvn ${PATH_TO_MAVEN}
```

### Running PCT with custom Java versions

Plugin compat tester supports running Test Suites with a Java version different 
from the one being used to run PCT and build the plugins.
For example, such mode can be used to run tests with JDK11 starting from Jenkins 2.163.

Two options can be passed to PCT CLI for such purpose:

* `testJDKHome` - A path to JDK HOME to be used for running tests in plugins
* `testJavaArgs` - Java test arguments to be used for test runs.
                   This option may be used to pass custom classpath and, in Java 11, additional modules

You can run the example by running the following command:

    make demo TEST_JDK_HOME=${YOUR_JDK_HOME} PLUGIN_NAME=git
    
When using the Docker image, it is possible to use `JDK_VERSION` variable to select 
the version to use. The version needs to be bundled in the docker image.
Currently Java 17 and Java 11 are bundled (JDK_VERSION= {17, 11}).
    
    make demo-docker JDK_VERSION=17 PLUGIN_NAME=git

Full list of options be found [here](./Makefile).

## Developer Info

### Debugging PCT in Docker

If you need to debug a dockerized PCT instance, 
use the `-e DEBUG=true -p 5005:5005` flags and then attach to the container using Remote Debugger in your IDE.

```
docker run --rm -v maven-repo:/root/.m2 -v $(pwd)/out:/pct/out -e ARTIFACT_ID=job-restrictions -e DEBUG=true -p 5005:5005 -i jenkins/pct
```

### Running PCT Makefiles on Windows

Plugin Compat Tester support Windows on its own,
but it might be tricky to get the development makefiles running there.
The default guidance it to use Docker, but it might be impossible in some cases.

One needs to install Make and Maven on Windows (e.g. using Chocolatey packages)
and then to properly setup the environment.

```batch
   set JAVA_HOME=...
   make demo PLUGIN_NAME=artifact-manager-s3 WAR_PATH=test-wars/mywar.war MVN_EXECUTABLE="C:\ProgramData\chocolatey\bin\mvn.exe"
```

## Useful links

* Devoxx '11 BOF Presentation about Plugin Compat Tester is available [here](http://www.slideshare.net/fcamblor/devoxx-2011-jenkins-bof-on-plugin-compatibility-tester). This presentation is partially obsolete (GAE feature was removed in recent versions)

