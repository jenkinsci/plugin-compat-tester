/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

def buildNumber = BUILD_NUMBER as int; if (buildNumber > 1) milestone(buildNumber - 1); milestone(buildNumber) // JENKINS-43353 / JENKINS-58625

// TODO: Move it to Jenkins Pipeline Library

/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
List platforms = ['linux', 'windows']
Map branches = [failFast: true]

for (int i = 0; i < platforms.size(); ++i) {
    String label = platforms[i]
    boolean publishing = (label == 'linux')
    branches[label] = {
        node(label) {
                stage('Checkout') {
                    if (isUnix()) { // have to clean the workspace as root
                        sh 'docker run --rm -v $(pwd):/src -w /src maven:3.6.0-jdk-8 sh -c "rm -rf * .[a-zA-Z]*" || :'
                    }
                    checkout scm
                }

                stage('Build') {
                  timeout(30) {
                    def args = ['clean', 'install', '-Dmaven.test.failure.ignore=true']
                    if (publishing) {
                      args += '-Dset.changelist'
                    }
                    infra.runMaven(args)
                  }
                }

                stage('Archive') {
                    /* Archive the test results */
                    junit '**/target/surefire-reports/TEST-*.xml'

                    if (publishing) {
                      findBugs pattern: '**/target/findbugsXml.xml'
                      infra.prepareToPublishIncrementals()
                    }
                }
        }
    }
}
parallel(branches)

// Integration testing, using a locally built Docker image
def itBranches = [:]


itBranches['buildtriggerbadge:2.10 tests success on JDK11'] = {
    node('docker') {
        checkout scm
        def settingsXML="mvn-settings.xml"
        infra.retrieveMavenSettingsFile(settingsXML)

        // should we build the image only once and somehow export and stash/unstash it then?
        // not sure this would be that quicker
        stage('Build Docker Image') {
            sh 'make docker'
        }

        stage('Download Jenkins 2.164.1') {
            sh '''
            curl -sL http://mirrors.jenkins.io/war-stable/2.164.1/jenkins.war --output jenkins.war
            echo "65543f5632ee54344f3351b34b305702df12393b3196a95c3771ddb3819b220b jenkins.war" | sha256sum --check
            '''
        }

        stage("Run known successful case(s)") {
            sh '''docker run --rm \
                         -v $(pwd)/jenkins.war:/pct/jenkins.war:ro \
                         -v $(pwd)/out:/pct/out -e JDK_VERSION=11 \
                         -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                         -e ARTIFACT_ID=buildtriggerbadge -e VERSION=buildtriggerbadge-2.10 \
                         jenkins/pct
            '''
            archiveArtifacts artifacts: "out/**"

            sh 'cat out/pct-report.html | grep "Tests : Success"'
        }
    }
}

itBranches['buildtriggerbadge:2.10 tests success on JDK8'] = {
    node('docker') {
        checkout scm
        def settingsXML="mvn-settings.xml"
        infra.retrieveMavenSettingsFile(settingsXML)

        // should we build the image only once and somehow export and stash/unstash it then?
        // not sure this would be that quicker
        stage('Build Docker Image') {
            sh 'make docker'
        }

        stage('Download Jenkins 2.164.1') {
            sh '''
            curl -sL http://mirrors.jenkins.io/war-stable/2.164.1/jenkins.war --output jenkins.war
            echo "65543f5632ee54344f3351b34b305702df12393b3196a95c3771ddb3819b220b jenkins.war" | sha256sum --check
            '''
        }

        stage("Run known successful case(s)") {
            sh '''docker run --rm \
                         -v $(pwd)/jenkins.war:/pct/jenkins.war:ro \
                         -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                         -v $(pwd)/out:/pct/out -e JDK_VERSION=8 \
                         -e ARTIFACT_ID=buildtriggerbadge -e VERSION=buildtriggerbadge-2.10 \
                         jenkins/pct
            '''
            archiveArtifacts artifacts: "out/**"

            sh 'cat out/pct-report.html | grep "Tests : Success"'
        }
    }
}


itBranches['WAR with non-default groupId plugins - smoke test'] = {
    node('docker') {
        checkout scm

        stage('Build Docker Image') {
          sh 'make docker'
        }

        dir("src/it/war-with-plugins-test") {
            def settingsXML="mvn-settings.xml"
            infra.retrieveMavenSettingsFile(settingsXML)

            stage('Build the custom WAR file') {
              infra.runMaven(["clean", "package"])
            }

            stage('Run the integration test') {
              sh '''docker run --rm \
                            -v $(pwd)/tmp/output/target/war-with-plugins-test-1.0.war:/pct/jenkins.war:ro \
                            -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                            -v $(pwd)/out:/pct/out -e JDK_VERSION=8 \
                            -e ARTIFACT_ID=artifact-manager-s3 -e VERSION=artifact-manager-s3-1.6 \
                            jenkins/pct \
                            -overridenPlugins 'io.jenkins:configuration-as-code=1.20'
              '''
              archiveArtifacts artifacts: "out/**"

              sh 'cat out/pct-report.html | grep "Tests : Success"'
            }
        }
    }
}

//TODO (oleg-nenashev): This step is unstable at the moment, see JENKINS-60583
Map disabled_itBranches = [:]
disabled_itBranches['CasC tests success'] = {
    node('linux') {
        checkout scm

        stage('Build PCT CLI') {
            withEnv([
                "JAVA_HOME=${tool 'jdk8'}",
                "PATH+MVN=${tool 'mvn'}/bin",
                'PATH+JDK=$JAVA_HOME/bin',
            ]) {
                sh 'make allNoDocker'
            }
        }

        stage("Run known successful case(s)") {
            withEnv([
                "JAVA_HOME=${tool 'jdk8'}",
                "MVN_PATH=${tool 'mvn'}/bin",
                "PATH+MVN=${tool 'mvn'}/bin",
                'PATH+JDK=$JAVA_HOME/bin',
            ]) {
                def settingsXML="mvn-settings.xml"
                infra.retrieveMavenSettingsFile(settingsXML)

                sh '''java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar \
                             -reportFile $(pwd)/out/pct-report.xml \
                             -workDirectory $(pwd)/out/work \
                             -skipTestCache true \
                             -mvn "$MVN_PATH/mvn" \
                             -m2SettingsFile $(pwd)/mvn-settings.xml \
                             -includePlugins configuration-as-code
                '''

                archiveArtifacts artifacts: "out/**"

                sh 'cat out/pct-report.html | grep "Tests : Success"'
            }
        }
    }
}

itBranches.failFast = false
parallel itBranches

infra.maybePublishIncrementals()
