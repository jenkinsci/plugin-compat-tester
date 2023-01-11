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

// Integration testing, using a locally built Docker image
def itBranches = [:]

itBranches['text-finder:1.22 tests success on JDK17'] = {
    node('docker') {
        infra.checkoutSCM()
        def settingsXML="mvn-settings.xml"
        infra.retrieveMavenSettingsFile(settingsXML)

        // should we build the image only once and somehow export and stash/unstash it then?
        // not sure this would be that quicker
        stage('Build Docker Image') {
            sh 'make docker'
        }

        stage('Download Jenkins 2.375.1') {
            sh '''
            curl -sSL https://get.jenkins.io/war-stable/2.375.1/jenkins.war -o jenkins.war
            echo "e96ae7f59d8a009bdbf3551d5e9facd97ff8a6d404c7ea2438ef267988d53427 jenkins.war" | sha256sum --check
            '''
        }

        stage("Run known successful case(s)") {
            sh '''docker run --rm \
                         -v $(pwd)/jenkins.war:/pct/jenkins.war:ro \
                         -v $(pwd)/out:/pct/out -e JDK_VERSION=17 \
                         -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                         -e ARTIFACT_ID=text-finder -e VERSION=text-finder-1.22 \
                         jenkins/pct
            '''
            archiveArtifacts artifacts: "out/**"

            sh 'cat out/pct-report.html | grep "Tests : Success"'
        }
    }
}

itBranches['text-finder:1.22 tests success on JDK11'] = {
    node('docker') {
        infra.checkoutSCM()
        def settingsXML="mvn-settings.xml"
        infra.retrieveMavenSettingsFile(settingsXML)

        // should we build the image only once and somehow export and stash/unstash it then?
        // not sure this would be that quicker
        stage('Build Docker Image') {
            sh 'make docker'
        }

        stage('Download Jenkins 2.375.1') {
            sh '''
            curl -sSL https://get.jenkins.io/war-stable/2.375.1/jenkins.war -o jenkins.war
            echo "e96ae7f59d8a009bdbf3551d5e9facd97ff8a6d404c7ea2438ef267988d53427 jenkins.war" | sha256sum --check
            '''
        }

        stage("Run known successful case(s)") {
            sh '''docker run --rm \
                         -v $(pwd)/jenkins.war:/pct/jenkins.war:ro \
                         -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                         -v $(pwd)/out:/pct/out -e JDK_VERSION=11 \
                         -e ARTIFACT_ID=text-finder -e VERSION=text-finder-1.22 \
                         jenkins/pct
            '''
            archiveArtifacts artifacts: "out/**"

            sh 'cat out/pct-report.html | grep "Tests : Success"'
        }
    }
}

itBranches.failFast = false
parallel itBranches

infra.maybePublishIncrementals()
