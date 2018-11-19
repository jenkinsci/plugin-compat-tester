#Makefile
TEST_JDK_HOME?=$(JAVA_HOME)
PLUGIN_NAME?=mailer

.PHONY: all
all: clean package docker

.PHONY: clean
clean:
	mvn clean

.PHONY: package
package:
	mvn package verify

.PHONY: docker
docker:
	docker build -t jenkins/pct .

tmp:
	mkdir tmp

tmp/jenkins.war: tmp
	curl -fsSL http://mirrors.jenkins.io/war/latest/jenkins.war -o tmp/jenkins.war

tmp/javax.activation.jar: tmp
	curl -fsSL http://central.maven.org/maven2/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar -o tmp/jaxb-api.jar
	curl -fsSL http://central.maven.org/maven2/com/sun/xml/bind/jaxb-core/2.3.0.1/jaxb-core-2.3.0.1.jar -o tmp/jaxb-core.jar
	curl -fsSL http://central.maven.org/maven2/com/sun/xml/bind/jaxb-impl/2.3.0.1/jaxb-impl-2.3.0.1.jar -o tmp/jaxb-impl.jar
	curl -fsSL https://github.com/javaee/activation/releases/download/JAF-1_2_0/javax.activation.jar -o tmp/javax.activation.jar

.PHONY: print-java-home
print-java-home:
	echo "Using JAVA_HOME for tests $(TEST_JDK_HOME)"

.PHONY: demo-jdk8 print-java-home
demo-jdk8:
	java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli-0.0.3-SNAPSHOT.jar \
	     -reportFile $(CURDIR)/out/pct-report.xml \
	     -workDirectory $(CURDIR)/work -skipTestCache true \
	     -mvn $(shell which mvn) -war tmp/jenkins.war \
	     -testJDKHome $(TEST_JDK_HOME) \
	     -includePlugins $(PLUGIN_NAME)

.PHONY: demo-jdk11
demo-jdk11: tmp/javax.activation.jar tmp/jenkins.war print-java-home
	# TODO Cleanup once the JAXB bundling issue is resolved.
	# https://issues.jenkins-ci.org/browse/JENKINS-52186
	java -jar plugins-compat-tester-cli/target/plugins-compat-tester-cli-0.0.3-SNAPSHOT.jar \
	     -reportFile $(CURDIR)/out/pct-report.xml \
	     -workDirectory $(CURDIR)/work -skipTestCache true \
	     -mvn $(shell which mvn) -war tmp/jenkins.war \
	     -testJDKHome $(TEST_JDK_HOME) \
	     -testJavaArgs "-p $(CURDIR)/tmp/jaxb-api.jar:$(CURDIR)/tmp/javax.activation.jar --add-modules java.xml.bind,java.activation -cp $(CURDIR)/tmp/jaxb-impl.jar:$(CURDIR)/tmp/jaxb-core.jar" \
	     -includePlugins $(PLUGIN_NAME)
