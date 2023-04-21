package org.jenkins.tools.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.io.FileMatchers.aReadableFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class PluginListerCliTest {

    @Test
    void testFileOutput(@TempDir File tempDir) throws IOException {
        PluginListerCli app = new PluginListerCli();
        CommandLine cmd = new CommandLine(app);
        File outputFile = new File(tempDir, "output.txt");
        int retVal = cmd.execute(
                "--war", new File("target", "megawar.war").getAbsolutePath(), "--output", outputFile.getAbsolutePath());
        assertEquals(retVal, 0);
        assertThat(outputFile, aReadableFile());
        List<String> plugins = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(plugins, is(List.of("jenkinsci/text-finder-plugin\ttext-finder")));
    }
}
