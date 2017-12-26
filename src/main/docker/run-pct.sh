#!/bin/bash
set -e
set -x

if [ $# -eq 1 ]; then
  # if `docker run` only has one argument, we assume that the user is running an alternate command 
  # like `bash` to inspect the image. All options passed to the executable shoud have at least 2 arguments.
  echo "Only one argument is specified, running a custom command"
  exec "$@"
  exit 0
fi

###
# Process arguments
###
if [ -n "${ARTIFACT_ID}" ]; then
  echo "Running PCT for plugin ${ARTIFACT_ID}"
else
  echo "ERROR: Artifact ID is not specified. Use environment variable, e.g. \"-e ARTIFACT_ID=credentials\""
  exit -1
fi

if [ -n "${CHECKOUT_SRC}" ] ; then
  echo "Using custom checkout source: ${CHECKOUT_SRC}"
else 
  CHECKOUT_SRC="https://github.com/jenkinsci/${ARTIFACT_ID}-plugin.git"
fi

if [ -n "${VERSION}"] ; then 
  VERSION="master"
fi

if [ -f "${JENKINS_WAR_PATH}" ]; then
  echo "Using custom Jenkins WAR from ${JENKINS_WAR_PATH}"
  mkdir -p "${PCT_TMP}"
  # WAR is accessed many times in the PCT runs, let's keep it local insead of pulling it from a volume
  cp "${JENKINS_WAR_PATH}" "${PCT_TMP}/jenkins.war"
  WAR_PATH_OPT="-war ${PCT_TMP}/jenkins.war "
else
  WAR_PATH_OPT=""
fi

###
# Checkout sources
###
mkdir -p "${PCT_TMP}/localCheckoutDir"
cd "${PCT_TMP}/localCheckoutDir"
if [ -e "/pct/plugin-src/pom.xml" ] ; then
  echo "Located custom plugin sources"
  mkdir "${ARTIFACT_ID}"
  cp -R /pct/plugin-src/* "${ARTIFACT_ID}/"
  # Due to whatever reason PCT blows up if you have work in the repo
  cd "${ARTIFACT_ID}" && mvn clean && rm -rf work
else
  echo "Checking out from ${CHECKOUT_SRC}:${VERSION}"
  git clone "${CHECKOUT_SRC}"
  mv $(ls .) ${ARTIFACT_ID}
  cd ${ARTIFACT_ID} && git checkout "${VERSION}"
fi

###
# Run PCT
###
cd /pct
PCT_CLI=$(ls src/plugins-compat-tester-cli/target/plugins-compat-tester-cli-*.jar )
if [ -e "${PCT_CLI}" ] ; then
  echo "Using PCT CLI ${PCT_CLI}"
else
  echo "Failed to locate PCT CLI"
  exit -1
fi

mkdir -p "${PCT_TMP}/work"
mkdir -p "${PCT_OUTPUT_DIR}"

# The image always uses external Maven due to https://issues.jenkins-ci.org/browse/JENKINS-48710
exec java -jar ${PCT_CLI} -reportFile ${PCT_OUTPUT_DIR}/pct-report.xml -workDirectory "${PCT_TMP}/work" ${WAR_PATH_OPT} -skipTestCache true -localCheckoutDir "${PCT_TMP}/localCheckoutDir/${ARTIFACT_ID}" -includePlugins "${ARTIFACT_ID}" -mvn "/usr/bin/mvn" "$@"
