properties([
    buildDiscarder(logRotator(numToKeepStr: '10')),
    disableConcurrentBuilds(abortPrevious: true)
])

// TODO: Move it to Jenkins Pipeline Library

/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
List platforms = ['linux', 'windows']
Map branches = [failFast: true]

for (int i = 0; i < platforms.size(); ++i) {
    String label = platforms[i]
    boolean publishing = (label == 'linux')
    def agentContainerLabel = 'maven-11'
    if (label == 'windows') {
      agentContainerLabel += '-windows'
    }
    branches[label] = {
        node(agentContainerLabel) {
                stage('Checkout') {
                    infra.checkoutSCM()
                }

                stage('Build') {
                  timeout(30) {
                    def args = ['clean', 'install', '-Dmaven.test.failure.ignore=true']
                    if (publishing) {
                      args += '-Dset.changelist'
                    }
                    infra.runMaven(args, 11)
                  }
                }

                stage('Archive') {
                    /* Archive the test results */
                    junit '**/target/surefire-reports/TEST-*.xml'

                    if (publishing) {
                      recordIssues(
                        enabledForFailure: true, aggregatingResults: true,
                        tools: [java(), spotBugs(pattern: '**/target/findbugsXml.xml')]
                      )
                      infra.prepareToPublishIncrementals()
                    }
                }
        }
    }
}
parallel(branches)

infra.maybePublishIncrementals()
