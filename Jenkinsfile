/* Only keep the 10 most recent builds. */
properties([[$class: 'BuildDiscarderProperty',
                strategy: [$class: 'LogRotator', numToKeepStr: '10']]])

// TODO: Move it to Jenkins Pipeline Library

/* These platforms correspond to labels in ci.jenkins.io, see:
 *  https://github.com/jenkins-infra/documentation/blob/master/ci.adoc
 */
List platforms = ['linux', 'windows']
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
                         -v $(pwd)/out:/pct/out -e JDK_VERSION=8 \
                         -e ARTIFACT_ID=buildtriggerbadge -e VERSION=buildtriggerbadge-2.10 \
                         jenkins/pct
            '''
            archiveArtifacts artifacts: "out/**"

            sh 'cat out/pct-report.html | grep "Tests : Success"'
        }
    }
}

itBranches['CasC tests success'] = {
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
                "PATH+MVN=${tool 'mvn'}/bin",
                'PATH+JDK=$JAVA_HOME/bin',
            ]) {
                sh '''java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar \
                             -reportFile $(pwd)/out/pct-report.xml \
                             -workDirectory $(pwd)/out/work \
                             -skipTestCache true \
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
