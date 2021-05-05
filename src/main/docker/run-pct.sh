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

explodeWARIfNeeded() {
  if [ ! -f "${PCT_TMP}/exploded/war" ]; then
    mkdir -p "${PCT_TMP}/exploded/war"
    unzip -o -q "${JENKINS_WAR_PATH}" -d "${PCT_TMP}/exploded/war"
  fi
}

###
# Process arguments
###
if [ -n "${ARTIFACT_ID}" ]; then
  echo "Running PCT for plugin ${ARTIFACT_ID}"
fi

CUSTOM_MAVEN_SETTINGS=${M2_SETTINGS_FILE:-"/pct/m2-settings.xml"}

# Set failOnError by default unless env FAIL_ON_ERROR is false
FAIL_ON_ERROR_ARG=""
if [ -n "${FAIL_ON_ERROR}" ]; then
    if [ "${FAIL_ON_ERROR}" != "false" ] ; then
        FAIL_ON_ERROR_ARG="-failOnError"
    fi
else
    FAIL_ON_ERROR_ARG="-failOnError"
fi

if [ -f "${CUSTOM_MAVEN_SETTINGS}" ] ; then
    echo "Using a custom Maven settings file"
    MVN_SETTINGS_FILE="${CUSTOM_MAVEN_SETTINGS}"
else
    MVN_SETTINGS_FILE="/pct/default-m2-settings.xml"
fi

if [ -n "${CHECKOUT_SRC}" ] ; then
  echo "Using custom checkout source: ${CHECKOUT_SRC}"
else
  if [ -z "${ARTIFACT_ID}" ] ; then
    if [ ! -e "/pct/plugin-src/pom.xml" ] ; then
      echo "Error: Plugin source is missing, cannot generate a default checkout path without ARTIFACT_ID"
      exit -1
    fi
  else
    CHECKOUT_SRC="https://github.com/jenkinsci/${ARTIFACT_ID}-plugin.git"
  fi
fi

if [ -f "${JENKINS_WAR_PATH}" ]; then
  mkdir -p "${PCT_TMP}"
  # WAR is accessed many times in the PCT runs, let's keep it local instead of pulling it from a volume
  cp "${JENKINS_WAR_PATH}" "${PCT_TMP}/jenkins.war"
  JENKINS_WAR_PATH="${PCT_TMP}/jenkins.war"
  WAR_PATH_OPT="-war ${PCT_TMP}/jenkins.war "
  JENKINS_VERSION=$(groovy /pct/scripts/readJenkinsVersion.groovy ${PCT_TMP}/jenkins.war)
  echo "Using custom Jenkins WAR v. ${JENKINS_VERSION} from ${JENKINS_WAR_PATH}"

  if [[ "$JENKINS_VERSION" =~ .*SNAPSHOT.* ]] ; then
    cd "${PCT_TMP}"
    echo "Version is a snapshot, will install artifacts to the local maven repo"
    explodeWARIfNeeded
    # TODO: Bug or feature?
    # Implementation-Version and Jenkins-Version may differ.
    # We specify version explicitly to use Jenkins-Version like PCT does
    if [ -z "${SKIP_LOCAL_SNAPSHOT_INSTALLATION}" ] ; then
        mvn -B org.apache.maven.plugins:maven-install-plugin:2.5:install-file --batch-mode ${JAVA_OPTS} -s "${MVN_SETTINGS_FILE}" \
            -Dpackaging=war -Dversion="${JENKINS_VERSION}" -Dfile="jenkins.war" \
            -DpomFile="exploded/war/META-INF/maven/org.jenkins-ci.main/jenkins-war/pom.xml"
        mvn -B org.apache.maven.plugins:maven-install-plugin:2.5:install-file --batch-mode ${JAVA_OPTS} -s "${MVN_SETTINGS_FILE}" \
            -Dpackaging=jar -Dfile="exploded/war/WEB-INF/lib/jenkins-core-${JENKINS_VERSION}.jar"
        mvn -B org.apache.maven.plugins:maven-install-plugin:2.5:install-file --batch-mode ${JAVA_OPTS} -s "${MVN_SETTINGS_FILE}" \
            -Dpackaging=jar -Dfile="exploded/war/WEB-INF/lib/cli-${JENKINS_VERSION}.jar"
        mvn -B org.apache.maven.plugins:maven-install-plugin:2.5:install-file --batch-mode -Dpackaging=pom ${JAVA_OPTS} -s "${MVN_SETTINGS_FILE}"  \
            -Dfile="exploded/war/META-INF/maven/org.jenkins-ci.main/jenkins-war/pom.xml" \
            -DpomFile="exploded/war/META-INF/maven/org.jenkins-ci.main/jenkins-war/pom.xml" \
            -Dversion="${JENKINS_VERSION}" -DartifactId="jenkins-war" -DgroupId="org.jenkins-ci.main"
        if [ "$INSTALL_BUNDLED_SNAPSHOTS" ] ; then
            # Install HPI, JAR and POM
            groovy /pct/scripts/installWARSnapshots.groovy "$(pwd)/exploded/war/WEB-INF/plugins" "$(pwd)/exploded" "${MVN_SETTINGS_FILE}" "${JAVA_OPTS}"
        fi
    fi
  fi
else
  WAR_PATH_OPT=""
fi

SHOULD_CHECKOUT=1
if [ -z "${VERSION}" ] ; then
  echo "Version is not not explicitly specified, will try to discover it from the WAR file"
  explodeWARIfNeeded
  HPI_PATH="${PCT_TMP}/exploded/war/WEB-INF/plugins/${ARTIFACT_ID}.hpi"
  if [ -f "${HPI_PATH}" ] ; then
    VERSION=$(groovy /pct/scripts/readJenkinsVersion.groovy "${HPI_PATH}" "Plugin-Version")
    SHOULD_CHECKOUT=0
  else
    VERSION="master"
  fi
fi
echo "Will be testing with ${ARTIFACT_ID}:${VERSION}, shouldCheckout=${SHOULD_CHECKOUT}"

extra_java_opts=()
if [[ "$DEBUG" ]] ; then
  extra_java_opts+=( \
    '-Xdebug' \
    '-Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y' \
  )
fi

LOCAL_CHECKOUT_ARG=""
if [ "${SHOULD_CHECKOUT}" -eq 1 ] && [ -z "${DO_NOT_OVERRIDE_PCT_CHECKOUT}" ] ; then
  ###
  # Checkout sources
  ###
  mkdir -p "${PCT_TMP}/localCheckoutDir"
  cd "${PCT_TMP}/localCheckoutDir"
  TMP_CHECKOUT_DIR="${PCT_TMP}/localCheckoutDir/undefined"
  if [ -e "/pct/plugin-src/pom.xml" ] ; then
    echo "Located custom plugin sources on the volume"
    mkdir "${TMP_CHECKOUT_DIR}"
    cp -r /pct/plugin-src/. "${TMP_CHECKOUT_DIR}"
    # Due to whatever reason PCT blows up if you have work in the repo
    cd "${TMP_CHECKOUT_DIR}" && mvn -B clean -s "${MVN_SETTINGS_FILE}" && rm -rf work
  else
    echo "Checking out from ${CHECKOUT_SRC}:${VERSION}"
    git clone "${CHECKOUT_SRC}"
    mv $(ls .) ${TMP_CHECKOUT_DIR}
    cd ${TMP_CHECKOUT_DIR} && git checkout "${VERSION}"
  fi

  ###
  # Determine artifact ID and then move the project to a proper location
  ###
  cd "${TMP_CHECKOUT_DIR}"
  if [ -z "${ARTIFACT_ID}" ] ; then
    ARTIFACT_ID=$(mvn -B org.apache.maven.plugins:maven-help-plugin:2.2:evaluate ${JAVA_OPTS} --batch-mode -s "${MVN_SETTINGS_FILE}" -Dexpression=project.artifactId | grep -Ev '(^\[|Download.*)')
    echo "ARTIFACT_ID is not specified, using ${ARTIFACT_ID} defined in the POM file"
    mvn -B clean -s "${MVN_SETTINGS_FILE}"
  fi
  mv "${TMP_CHECKOUT_DIR}" "${PCT_TMP}/localCheckoutDir/${ARTIFACT_ID}"
  LOCAL_CHECKOUT_ARG="-localCheckoutDir ${PCT_TMP}/localCheckoutDir/${ARTIFACT_ID}"
fi


mkdir -p "${PCT_TMP}/work"
mkdir -p "${PCT_OUTPUT_DIR}"

###
# Determine if we test the plugin against another JDK
###
TEST_JDK_HOME=${TEST_JAVA_ARGS:-"/usr/local/openjdk-${JDK_VERSION:-8}"}
TEST_JAVA_ARGS="'${TEST_JAVA_ARGS:-} -Xmx768M -Djava.awt.headless=true -Djdk.net.URLClassPath.disableClassPathURLCheck=true'"

pctExitCode=0
echo java ${JAVA_OPTS} ${extra_java_opts[@]} \
  -jar /pct/pct-cli.jar \
  -reportFile ${PCT_OUTPUT_DIR}/pct-report.xml \
  -workDirectory "${PCT_TMP}/work" ${WAR_PATH_OPT} \
  -skipTestCache true \
  ${FAIL_ON_ERROR_ARG}\
  ${LOCAL_CHECKOUT_ARG} \
  -includePlugins "${ARTIFACT_ID}" \
  -m2SettingsFile "${MVN_SETTINGS_FILE}" \
  -testJDKHome "${TEST_JDK_HOME}" \
  -testJavaArgs ${TEST_JAVA_ARGS:-} \
  "$@" \
  "|| echo \$? > /pct/tmp/pct_exit_code" > /pct/tmp/pct_command
chmod +x /pct/tmp/pct_command
cat /pct/tmp/pct_command
sh -ex /pct/tmp/pct_command || "Execution failed with code $?"

if [ -f "/pct/tmp/pct_exit_code" ]; then
  pctExitCode=$(cat "/pct/tmp/pct_exit_code")
fi

if [[ "${pctExitCode}" != "0" ]] ; then
  echo "ERROR: PCT failed with code ${pctExitCode}. Will check for Maven Surefire dumps if it crashed"
  dumpFiles=$(find /pct/tmp/work/${ARTIFACT_ID}/target/surefire-reports/*.dump* || echo "")
  if [ -n "${dumpFiles}" ] ; then
    echo "${dumpFiles}" | while read file; do
      echo "Found Maven Surefire dump file: ${file}"
      cat "${file}"
    done
  fi
  exit "${pctExitCode}"
fi
