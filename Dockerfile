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

FROM maven:3.5.4-jdk-8 as builder

COPY plugins-compat-tester/ /pct/src/plugins-compat-tester/
COPY plugins-compat-tester-cli/ /pct/src/plugins-compat-tester-cli/
COPY plugins-compat-tester-gae/ /pct/src/plugins-compat-tester-gae/
COPY plugins-compat-tester-gae-client/ /pct/src/plugins-compat-tester-gae-client/
COPY plugins-compat-tester-model/ /pct/src/plugins-compat-tester-model/
COPY *.xml /pct/src/
COPY LICENSE.txt /pct/src/LICENSE.txt

WORKDIR /pct/src/
RUN mvn clean install -DskipTests

FROM maven:3.5.4-jdk-8
MAINTAINER Oleg Nenashev <o.v.nenashev@gmail.com>
LABEL Description="Base image for running Jenkins Plugin Compat Tester (PCT) against custom plugins and Jenkins cores" Vendor="Jenkins project"
ENV JENKINS_WAR_PATH=/pct/jenkins.war
ENV PCT_OUTPUT_DIR=/pct/out
ENV PCT_TMP=/pct/tmp
ENV INSTALL_BUNDLED_SNAPSHOTS=true

RUN apt-get -y update && apt-get install -y groovy && rm -rf /var/lib/apt/lists/*

COPY src/main/docker/*.groovy /pct/scripts/
COPY --from=builder /pct/src/plugins-compat-tester-cli/target/plugins-compat-tester-cli-*.jar /pct/pct-cli.jar
COPY src/main/docker/run-pct.sh /usr/local/bin/run-pct
COPY src/main/docker/pct-default-settings.xml /pct/default-m2-settings.xml

EXPOSE 5005

VOLUME /pct/plugin-src
VOLUME /pct/jenkins.war
VOLUME /pct/out
VOLUME /pct/tmp
VOLUME /pct/m2-settings.xml
VOLUME /root/.m2
ENTRYPOINT ["run-pct"]
