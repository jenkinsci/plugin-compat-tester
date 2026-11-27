package org.jenkins.tools.test.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExpressionEvaluatorTest {

    @Test
    void readsOnlyStringElementsAndDeletesTheOutput(@TempDir Path directory) throws Exception {
        MavenRunner runner = (properties, baseDirectory, module, buildLogFile, args) -> {
            try {
                Files.writeString(
                        Path.of(properties.get("output")),
                        "<strings>\n  <string>first</string>\n\t<string>second</string>\n"
                                + "\nnoise<string>ignored</string>\n<STRING>ignored</STRING>\n"
                                + "<string></string>\n</strings>\n",
                        Charset.defaultCharset());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        ExpressionEvaluator evaluator = new ExpressionEvaluator(directory.toFile(), null, runner);
        assertEquals(List.of("first", "second", ""), evaluator.evaluateList("items"));
        assertFalse(Files.exists(directory.resolve("items.log")));
    }
}
