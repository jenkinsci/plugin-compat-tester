#!/usr/bin/env groovy
import java.util.zip.*
import java.util.jar.*

class PluginData {
    String groupId
    String artifactId
    String version
}

static void installPluginSnapshots(File directory, File explodeDir, File mavenSettings = null, String javaOpts="") throws IOException {
    def plugins = directory.listFiles()
    if (plugins == null) {
        return
    }

    explodeDir.mkdirs()
    for (File plugin : plugins) {
        if (plugin.getAbsolutePath().endsWith(".hpi")) {
            File pluginExplodeDir = new File(explodeDir, plugin.name)
            installLocally(plugin, pluginExplodeDir, mavenSettings, javaOpts)
        }
    }
}

static void installLocally(File plugin, File tmpDir, File mavenSettings = null, String javaOpts="") {
    def pluginData = readPluginManifest(plugin)
    if (!pluginData.version.contains("SNAPSHOT")) {
        return
    }
    println "Installing SNAPSHOT for ${pluginData.artifactId}:${pluginData.version}"
    execOrFail("unzip -q ${plugin.absolutePath} -d ${tmpDir.absolutePath}")

    String mvnSettingsArg = mavenSettings != null ? "-s ${mavenSettings.absolutePath}" : ""
    execOrFail("mvn org.apache.maven.plugins:maven-install-plugin:2.5:install-file " +
        "--batch-mode ${javaOpts} ${mvnSettingsArg} " +
        "-Dfile=${plugin.absolutePath}")
    execOrFail("mvn org.apache.maven.plugins:maven-install-plugin:2.5:install-file " +
        "--batch-mode -Dpackaging=jar ${javaOpts} ${mvnSettingsArg} " +
        "-Dfile=${tmpDir.absolutePath}/WEB-INF/lib/${pluginData.artifactId}.jar")
    execOrFail("mvn org.apache.maven.plugins:maven-install-plugin:2.5:install-file " +
        "--batch-mode -Dpackaging=pom ${javaOpts} ${mvnSettingsArg} " +
        "-Dfile=${tmpDir.absolutePath}/META-INF/maven/${pluginData.groupId}/${pluginData.artifactId}/pom.xml " +
        "-DpomFile=${tmpDir.absolutePath}/META-INF/maven/${pluginData.groupId}/${pluginData.artifactId}/pom.xml " +
        "-Dversion=${pluginData.version} -DartifactId=pom -DgroupId=${pluginData.groupId}")
}

static void execOrFail(String command) {
    def proc = command.execute()
    proc.consumeProcessOutput(System.out, System.err)
    proc.waitForOrKill(5000)
    if (proc.exitValue() != 0) {
       throw new IOException("Task failed with exit code ${proc.exitValue()}")
    }
}

static PluginData readPluginManifest(File sourceHPI) throws IOException {
    def zipFile = new ZipFile(sourceHPI)
    InputStream is
    try {
        def entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement()
            if (zipEntry.getName().equals("META-INF/MANIFEST.MF")) {
                is = zipFile.getInputStream(zipEntry)
                def manifest = new Manifest(is)
                def mainAttribs = manifest.getMainAttributes()
                PluginData res = new PluginData()
                res.version = mainAttribs.getValue("Plugin-Version").split("\\s+")[0]
                res.groupId = mainAttribs.getValue("Group-Id")
                res.artifactId = mainAttribs.getValue("Short-Name")
                return res
            }
        }
    } finally {
        if (is != null) {
            is.close()
        }
        zipFile.close()
    }

    throw new IllegalStateException("Manifest not found in ${sourceHPI}")
}

println installPluginSnapshots(new File(this.args[0]), new File((String)this.args[1]),
    this.args.length > 2 ? new File(this.args[2]) : null,
    this.args.length > 3 ? this.args[3] : "")
