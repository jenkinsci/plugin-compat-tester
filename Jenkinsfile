/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

// TODO: Move it to Jenkins Pipeline Library

/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
// TODO: restore
List platforms = [] //'linux', 'windows']
Map branches = [:]

for (int i = 0; i < platforms.size(); ++i) {
    String label = platforms[i]
    branches[label] = {
        node(label) {
            timestamps {
                stage('Checkout') {
                    checkout scm
                }

                stage('Build') {
                    withEnv([
                        "JAVA_HOME=${tool 'jdk8'}",
                        "PATH+MVN=${tool 'mvn'}/bin",
                        'PATH+JDK=$JAVA_HOME/bin',
                    ]) {
                        timeout(30) {
                            String command = 'mvn --batch-mode clean install -Dmaven.test.failure.ignore=true'
                            if (isUnix()) {
                                sh command
                            }
                            else {
                                bat command
                            }
                        }
                    }
                }

                stage('Archive') {
                    /* Archive the test results */
                    junit '**/target/surefire-reports/TEST-*.xml'

                    if (label == 'linux') {
                      archiveArtifacts artifacts: '**/target/**/*.jar'
                      findbugs pattern: '**/target/findbugsXml.xml'
                    }
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

        stage("Run integration tests") {
            
        }
    }
}

itBranches['WAR with Plugins - smoke test'] = {
    node('docker') {
        checkout scm
        dir("src/it/war-with-plugins-test") {
            def settingsXML="mvn-settings.xml"
            infra.retrieveMavenSettingsFile(settingsXML)
            
            infra.runMaven(["clean", "package"])
            sh '''docker run --rm \
                            -v $(pwd)/tmp/output/target/war-with-plugins-test-1.0.war:/pct/jenkins.war:ro \
                            -v $(pwd)/mvn-settings.xml:/pct/m2-settings.xml \
                            -v $(pwd)/out:/pct/out -e JDK_VERSION=8 \
                            -e ARTIFACT_ID=artifact-manager-s3 -e VERSION=artifact-manager-s3-1.6 \
                            jenkins/pct \
                            -overridenPlugins 'configuration-as-code=1.20'
            '''
        }
    }
}

itBranches.failFast = false
parallel itBranches
