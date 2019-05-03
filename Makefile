#Makefile
TEST_JDK_HOME?=$(JAVA_HOME)
PLUGIN_NAME?=mailer

JENKINS_VERSION=2.164.2

.PHONY: all
all: clean package docker

.PHONY: clean
clean:
	mvn clean

plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar:
	mvn verify
.PHONY: package
package: plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar

.PHONY: docker
docker: Dockerfile 
	docker build -t jenkins/pct .

tmp:
	mkdir tmp

.PRECIOUS: tmp/jenkins-war-$(JENKINS_VERSION).war
tmp/jenkins-war-$(JENKINS_VERSION).war: tmp
	mvn dependency:copy -Dartifact=org.jenkins-ci.main:jenkins-war:$(JENKINS_VERSION):war -DoutputDirectory=tmp
	touch tmp/jenkins-war-$(JENKINS_VERSION).war

.PHONY: print-java-home
print-java-home:
	echo "Using JAVA_HOME for tests $(TEST_JDK_HOME)"

.PHONY: demo-jdk8
demo-jdk8: plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar tmp/jenkins-war-$(JENKINS_VERSION).war print-java-home
	java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar \
	     -reportFile $(CURDIR)/out/pct-report.xml \
	     -failOnError \
	     -workDirectory $(CURDIR)/work \
	     -skipTestCache true \
	     -mvn $(shell which mvn) \
	     -war $(CURDIR)/tmp/jenkins-war-$(JENKINS_VERSION).war \
	     -testJDKHome $(TEST_JDK_HOME) \
	     -includePlugins $(PLUGIN_NAME)

.PHONY: demo-jdk11
demo-jdk11: plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar tmp/jenkins-war-$(JENKINS_VERSION).war print-java-home
	# TODO Cleanup when/if the JAXB bundling issue is resolved.
	# https://issues.jenkins-ci.org/browse/JENKINS-52186
	java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar \
	     -reportFile $(CURDIR)/out/pct-report.xml \
	     -failOnError \
	     -workDirectory $(CURDIR)/work \
	     -skipTestCache true \
	     -mvn $(shell which mvn) \
	     -war $(CURDIR)/tmp/jenkins-war-$(JENKINS_VERSION).war \
	     -testJDKHome $(TEST_JDK_HOME) \
	     -includePlugins $(PLUGIN_NAME)

# We do not automatically rebuild Docker here
.PHONY: demo-jdk11-docker
demo-jdk11-docker: tmp/jenkins-war-$(JENKINS_VERSION).war
	docker run --rm -v maven-repo:/root/.m2 \
	     -v $(shell pwd)/out:/pct/out \
	     -v $(shell pwd)/tmp/jenkins-war-$(JENKINS_VERSION).war:/pct/jenkins.war:ro \
	     -e ARTIFACT_ID=$(PLUGIN_NAME) \
	     -e JDK_VERSION=11 \
	     jenkins/pct

.PHONY: demo-jdk11-docker-src
demo-jdk11-docker-src: tmp/jenkins-war-$(JENKINS_VERSION).war
	docker run --rm -v maven-repo:/root/.m2 \
	     -v $(shell pwd)/out:/pct/out \
	     -v $(shell pwd)/work/$(PLUGIN_NAME):/pct/plugin-src:ro \
	     -v $(shell pwd)/tmp/jenkins-war-$(JENKINS_VERSION).war:/pct/jenkins.war:ro \
	     -e ARTIFACT_ID=$(PLUGIN_NAME) \
	     -e JDK_VERSION=11 \
	     jenkins/pct
