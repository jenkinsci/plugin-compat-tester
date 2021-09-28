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

FROM maven:3.8.1-jdk-8 as builder

# Warmup to avoid downloading the world each time
RUN git clone https://github.com/jenkinsci/plugin-compat-tester &&\
    cd plugin-compat-tester && \
    mvn clean package -Dmaven.test.skip=true dependency:go-offline && \
    mvn clean

COPY plugins-compat-tester/ /pct/src/plugins-compat-tester/
COPY plugins-compat-tester-cli/ /pct/src/plugins-compat-tester-cli/
COPY plugins-compat-tester-model/ /pct/src/plugins-compat-tester-model/
COPY *.xml /pct/src/
COPY LICENSE.txt /pct/src/LICENSE.txt

WORKDIR /pct/src/
RUN mvn clean package -Dmaven.test.skip=true

FROM maven:3.8.1-jdk-8
LABEL Maintainer="Oleg Nenashev <o.v.nenashev@gmail.com>"
LABEL Description="Base image for running Jenkins Plugin Compat Tester (PCT) against custom plugins and Jenkins cores" Vendor="Jenkins project"
ENV JENKINS_WAR_PATH=/pct/jenkins.war
ENV PCT_OUTPUT_DIR=/pct/out
ENV PCT_TMP=/pct/tmp
ENV INSTALL_BUNDLED_SNAPSHOTS=true

RUN apt-get -y update && apt-get install -y groovy apt-transport-https ca-certificates gnupg2 software-properties-common && rm -rf /var/lib/apt/lists/*

RUN curl -L --show-error https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz --output openjdk.tar.gz && \
    echo "7a6bb980b9c91c478421f865087ad2d69086a0583aeeb9e69204785e8e97dcfd  openjdk.tar.gz" | sha256sum -c && \
    tar xvzf openjdk.tar.gz && \
    mv jdk-11.0.1/ /usr/local/openjdk-11 && \
    rm openjdk.tar.gz

RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt-get -y update && apt-get install -y docker-ce docker-ce-cli containerd.io

COPY src/main/docker/*.groovy /pct/scripts/
COPY --from=builder /pct/src/plugins-compat-tester-cli/target/plugins-compat-tester-cli.jar /pct/pct-cli.jar
COPY src/main/docker/run-pct.sh /usr/local/bin/run-pct
COPY src/main/docker/pct-default-settings.xml /pct/default-m2-settings.xml

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
