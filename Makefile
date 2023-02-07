#Makefile
TEST_JDK_HOME?=$(JAVA_HOME)
PLUGIN_NAME?=mailer
JDK_VERSION?=17
# Relative path to the WAR file to be used for tests
WAR_PATH?=tmp/jenkins-war-$(JENKINS_VERSION).war
# Extra options to pass to PCT, works only in the local steps
EXTRA_OPTS?=
# Maven executable for local runs. Can be automatically discovered on Linux, but requires manual specification when running on Windows
MVN_EXECUTABLE?=$(shell which mvn)


JENKINS_VERSION=2.375.1

.PHONY: all
all: clean package docker

.PHONY: allNoDocker
allNoDocker: clean package

.PHONY: clean
clean:
	mvn clean

target/plugins-compat-tester-cli.jar:
	mvn package
.PHONY: package
package: target/plugins-compat-tester-cli.jar

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

.PHONY: demo
demo: target/plugins-compat-tester-cli.jar $(WAR_PATH) print-java-home
	java -jar target/plugins-compat-tester-cli.jar \
	     -workDirectory $(CURDIR)/work \
	     -mvn $(MVN_EXECUTABLE) \
	     -war $(CURDIR)/$(WAR_PATH) \
	     -includePlugins $(PLUGIN_NAME) \
	     $(EXTRA_OPTS)

# We do not automatically rebuild Docker here
.PHONY: demo-docker
demo-docker: tmp/jenkins-war-$(JENKINS_VERSION).war
	docker run --rm -v maven-repo:/root/.m2 \
	     -v $(CURDIR)/out:/pct/out \
	     -v $(CURDIR)/$(WAR_PATH):/pct/jenkins.war:ro \
	     -e ARTIFACT_ID=$(PLUGIN_NAME) \
	     -e JDK_VERSION=$(JDK_VERSION) \
	     jenkins/pct

.PHONY: demo-docker-src
demo-docker-src: $(WAR_PATH)
	docker run --rm -v maven-repo:/root/.m2 \
	     -v $(CURDIR)/out:/pct/out \
	     -v $(CURDIR)/work/$(PLUGIN_NAME):/pct/plugin-src:ro \
	     -v $(CURDIR)/$(WAR_PATH):/pct/jenkins.war:ro \
	     -e ARTIFACT_ID=$(PLUGIN_NAME) \
	     -e JDK_VERSION=$(JDK_VERSION) \
	     jenkins/pct
