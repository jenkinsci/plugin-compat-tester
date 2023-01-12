# The MIT License
#
#  Copyright (c) 2017-2018 CloudBees, Inc.
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in
#  all copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#  THE SOFTWARE.

FROM eclipse-temurin:11-jdk AS jdk11-build
FROM groovy:4.0-jdk17 AS groovy-build
FROM maven:3.8.7-eclipse-temurin-17

LABEL Description="Base image for running Jenkins Plugin Compat Tester (PCT) against custom plugins and Jenkins cores" Vendor="Jenkins project"
ENV JENKINS_WAR_PATH=/pct/jenkins.war
ENV PCT_OUTPUT_DIR=/pct/out
ENV PCT_TMP=/pct/tmp
ENV INSTALL_BUNDLED_SNAPSHOTS=true

RUN apt-get update \
  && apt-get install -y --no-install-recommends unzip \
  && rm -rf /var/lib/apt/lists/*

COPY --from=jdk11-build /opt/java/openjdk /opt/java/openjdk-11

ENV GROOVY_HOME /opt/groovy
COPY --from=groovy-build /opt/groovy $GROOVY_HOME
RUN ln -s "${GROOVY_HOME}/bin/grape" /usr/bin/grape \
  && ln -s "${GROOVY_HOME}/bin/groovy" /usr/bin/groovy \
  && ln -s "${GROOVY_HOME}/bin/groovyc" /usr/bin/groovyc \
  && ln -s "${GROOVY_HOME}/bin/groovyConsole" /usr/bin/groovyConsole \
  && ln -s "${GROOVY_HOME}/bin/groovydoc" /usr/bin/groovydoc \
  && ln -s "${GROOVY_HOME}/bin/groovysh" /usr/bin/groovysh \
  && ln -s "${GROOVY_HOME}/bin/java2groovy" /usr/bin/java2groovy

COPY src/ /pct/src/src/
COPY *.xml /pct/src/
COPY LICENSE.txt /pct/src/LICENSE.txt
COPY src/main/docker/*.groovy /pct/scripts/
COPY src/main/docker/run-pct.sh /usr/local/bin/run-pct
COPY src/main/docker/pct-default-settings.xml /pct/default-m2-settings.xml

RUN cd /pct/src \
  && mvn -B -V -ntp clean package -DskipTests \
  && cp target/plugins-compat-tester-cli.jar /pct/pct-cli.jar

# TODO: remove .git addition and generate a commit.txt file once we don't use Docker Hub anymore
ADD .git /tmp/repo
RUN cd /tmp/repo && git rev-parse HEAD > /commit.txt && rm -rf /tmp/repo

EXPOSE 5005

VOLUME /pct/plugin-src
VOLUME /pct/jenkins.war
VOLUME /pct/out
VOLUME /pct/tmp
VOLUME /pct/m2-settings.xml
VOLUME /root/.m2
ENTRYPOINT ["run-pct"]
