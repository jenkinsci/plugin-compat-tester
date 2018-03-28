import java.util.zip.*
import java.util.jar.*

static String readManifest(String sourceJARFile) throws IOException {
    def zipFile = new ZipFile(sourceJARFile)
    InputStream is
    try {
        def entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement()
            if (zipEntry.getName().equals("META-INF/MANIFEST.MF")) {
                is = zipFile.getInputStream(zipEntry)
                def manifest = new Manifest(is)
                def mainAttribs = manifest.getMainAttributes()
                def version = mainAttribs.getValue("Jenkins-Version")
                if(version != null) {
                    return version
                }
            }
        }
    } finally {
        if (is != null) {
            is.close()
        }
        zipFile.close()
    }

    throw new IllegalStateException("Manifest not found in" + sourceJARFile);
}

println readManifest(this.args[0])
