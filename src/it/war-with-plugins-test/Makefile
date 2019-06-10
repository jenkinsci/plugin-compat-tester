#Makefile
TEST_NAME=war-with-plugins-test
TEST_VERSION=1.0
CUSTOM_JENKINS_WAR=tmp/output/target/$(TEST_NAME)-$(TEST_VERSION).war

.PRECIOUS: $(CUSTOM_JENKINS_WAR)
$(CUSTOM_JENKINS_WAR): tmp
	mvn clean package
	touch $(CUSTOM_JENKINS_WAR).war

out:
	mkdir out

.PHONY: test
test: out
	docker run -ti --rm -v maven-repo:/root/.m2 \
	    -v $(CURDIR)/out:/pct/out \
	    -v $(CURDIR)/$(CUSTOM_JENKINS_WAR):/pct/jenkins.war:ro \
	    -e ARTIFACT_ID=artifact-manager-s3 \
	    -e VERSION=artifact-manager-s3-1.6 \
	    jenkins/pct \
	    -overridenPlugins 'configuration-as-code=1.20' -overridenPlugins 'workflow-step-api=2.18'

clean:
	rm -rf out
	rm =rf tmp
