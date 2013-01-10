Generate a compatibility matrix for plugins against Jenkins core.

See https://wiki.jenkins-ci.org/display/JENKINS/Plugin+Compatibility+Tester for background.

To do:

1. (optionally) scan `jenkins.war` for plugins (and core version) rather than loading update center, so as to test that all bundled plugins are actually compatible
2. use `-Djenkins.version` (& `-Dhpi-plugin.version`) rather than editing POM parent version, when using a parent POM that supports this (`org.jenkins-ci.plugins:plugin` does not yet but it has been recommended)
3. unpack the released `*.hpi` into the right spot in the `target` dir, then run `mvn surefire:test` to run tests against released binaries, to catch binary compat errors (then still run `mvn clean test` to catch source compat errors)
