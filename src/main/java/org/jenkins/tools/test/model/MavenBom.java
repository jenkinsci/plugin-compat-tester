package org.jenkins.tools.test.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenBom {

    private static final Logger LOGGER = Logger.getLogger(MavenBom.class.getName());

    private Model contents;

    public MavenBom(File path) throws IOException, XmlPullParserException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8))) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            contents = reader.read(in);
        }
    }

    public Model getModel() {
        return contents;
    }

    public File writeFullDepPom(File workDir) throws IOException {
        Model modified = getModel().clone();
        File fullDepPom = new File(workDir, "bom/pom.xml");
        FileUtils.forceMkdir(fullDepPom.getParentFile());
        List<Dependency> managed = getModel().getDependencyManagement().getDependencies();
        for (Dependency dep : managed) {
            if (!this.containsDep(dep, modified)) {
                modified.addDependency(dep);
            }
        }
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (FileOutputStream out = new FileOutputStream(fullDepPom)) {
            writer.write(out, modified);
            return fullDepPom;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write full dependency POM; continuing", e);
        }
        return null;
    }

    public boolean containsDep(Dependency toCheck, Model model) {
        return model.getDependencies().stream().anyMatch(dep -> getDepDescription(dep).equals(getDepDescription(toCheck)));
    }

    private String getDepDescription(Dependency toCheck) {
        return String.join(":", toCheck.getGroupId(), toCheck.getArtifactId(), toCheck.getVersion(), toCheck.getType(),
                toCheck.getClassifier(), toCheck.getScope());
    }
}
