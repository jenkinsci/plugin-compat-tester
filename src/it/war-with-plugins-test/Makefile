#Makefile
TEST_NAME=war-with-plugins-test
TEST_VERSION=1.0
CUSTOM_JENKINS_WAR=tmp/output/target/$(TEST_NAME)-$(TEST_VERSION).war

.PRECIOUS: $(CUSTOM_JENKINS_WAR)
$(CUSTOM_JENKINS_WAR): tmp
	mvn clean package
	touch $(CUSTOM_JENKINS_WAR).war

tmp:
	mkdir tmp

out:
	mkdir out

.PHONY: build
build: $(CUSTOM_JENKINS_WAR)

.PHONY: run
run:
	JENKINS_HOME=$(CURDIR)/work java -jar tmp/output/target/war-with-plugins-test-1.0.war

.PHONY: test
test: out $(CUSTOM_JENKINS_WAR)
	docker run -ti --rm -v maven-repo:/root/.m2 \
	    -v $(CURDIR)/out:/pct/out \
	    -v $(CURDIR)/$(CUSTOM_JENKINS_WAR):/pct/jenkins.war:ro \
	    -e ARTIFACT_ID=artifact-manager-s3 \
	    -e VERSION=artifact-manager-s3-1.6 \
	    jenkins/pct \
	    -overridenPlugins 'configuration-as-code=1.20'

clean:
	rm -rf out
	rm -rf tmp
