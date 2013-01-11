Generate a compatibility matrix for plugins against Jenkins core.

See https://wiki.jenkins-ci.org/display/JENKINS/Plugin+Compatibility+Tester for background.

To do:

1. prior to 1.485 plugin POMs were not consistently deployed so must strip micro version e.g. 1.480.2 -> 1.480 (try `mvn org.apache.maven.plugins:maven-dependency-plugin:2.5.1:get -Dartifact=org.jenkins-ci.plugins:plugin:$version:pom -Dtransitive=false`)
1. use `-Djenkins.version` (& `-Dhpi-plugin.version`) rather than editing POM parent version, when using a parent POM that supports this (`org.jenkins-ci.plugins:plugin` does not yet but it has been recommended)
1. unpack the released `*.hpi` into the right spot in the `target` dir, then run `mvn surefire:test` to run tests against released binaries, to catch binary compat errors (then still run `mvn clean test` to catch source compat errors)
1. currently still seems to run `install` goal which is very undesirable on release tags
1. should run `surefire-report:report` goal instead (or `surefire-report:report-only` after) and display link to HTML results from index page
1. Export *everything* to GAE, dropping the data storing in XML files (which pollutes the filesystem and can be easily delete if we are careless) and processing with XSL. (migration already started to GAE datastorage, but not completely finished, especially on build logs).
1. Improve GAE app to allow plugin maintainers to subscribe to notifications on plugin compatibility tests for their plugins against new jenkins versions released.
1. Remove possibility, on GAE app, to select both "every plugins" and "every cores" results... because it generates too much results and crash GAE datastore
1. most plugin tests fail to build using internal Maven; `PlexusWagonProvider.lookup` with a `roleHint=https` fails for no clear reason, and some missing `SNAPSHOT`s cause a build failure related to https://github.com/stapler/stapler-adjunct-codemirror/commit/da995b03a1f165fef7c9d34eadb15797f58399cd
