Generate a compatibility matrix for plugins against Jenkins core.

See https://wiki.jenkins-ci.org/display/JENKINS/Plugin+Compatibility+Tester for background.

To do:
# most plugin tests fail to build in 1.480 due to a Maven/Aether error from `DefaultRemoteRepositoryManager` maybe caused by unlogged error in `WagonRepositoryConnector` (below)
# prior to 1.485 plugin POMs were not consistently deployed so must strip micro version e.g. 1.480.2 -> 1.480 (try `mvn org.apache.maven.plugins:maven-dependency-plugin:2.5.1:get -Dartifact=org.jenkins-ci.plugins:plugin:$version:pom -Dtransitive=false`)
# use `-Djenkins.version` (& `-Dhpi-plugin.version`) rather than editing POM parent version, when using a parent POM that supports this (`org.jenkins-ci.plugins:plugin` does not yet but it has been recommended)
# unpack the released `*.hpi` into the right spot in the `target` dir, then run `mvn surefire:test` to run tests against released binaries, to catch binary compat errors (then still run `mvn clean test` to catch source compat errors)
# currently still seems to run `install` goal which is very undesirable on release tags
# should run `surefire-report:report` goal instead (or `surefire-report:report-only` after) and display link to HTML results from index page

    org.sonatype.aether.transfer.ArtifactTransferException: Failure to transfer org.kohsuke.stapler:stapler:pom:1.176-SNAPSHOT
      from http://maven.jenkins-ci.org/content/repositories/snapshots/ was cached in the local repository,
      resolution will not be reattempted until the update interval of maven.jenkins-ci.org has elapsed or updates are forced.
      Original error: Could not transfer artifact org.kohsuke.stapler:stapler:pom:1.176-SNAPSHOT from/to maven.jenkins-ci.org
      (http://maven.jenkins-ci.org/content/repositories/snapshots/): No connector available to access repository maven.jenkins-ci.org
      (http://maven.jenkins-ci.org/content/repositories/snapshots/) of type default using the available factories WagonRepositoryConnectorFactory
        at org.sonatype.aether.impl.internal.DefaultUpdateCheckManager.newException(DefaultUpdateCheckManager.java:237)
