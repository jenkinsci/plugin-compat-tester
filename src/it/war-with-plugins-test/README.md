Integration test for Jenkins WAR with bundled plugins
=====================================================

The test uses [Custom WAR Packager](https://github.com/jenkinsci/custom-war-packager) as a tool to build a custom WAR.
It basically implements a smoke test for the [JENKINS-57935](https://issues.jenkins-ci.org/browse/JENKINS-57935) fix 
which is related to improper `groupId` resolution for optional dependencies.

Running locally:

```
    make test
```

