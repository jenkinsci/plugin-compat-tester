package org.jenkins.tools.test.maven;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.jenkins.tools.test.exception.PomExecutionException;

/**
 * A high-level wrapper over {@link MavenRunner} that allows for the evaluation of arbitrary expressions.
 */
public class ExpressionEvaluator {

    @NonNull
    private final File pluginPath;

    @NonNull
    private final MavenRunner runner;

    public ExpressionEvaluator(File pluginPath, MavenRunner runner) {
        this.pluginPath = pluginPath;
        this.runner = runner;
    }

    public String evaluateString(String expression) throws PomExecutionException {
        Path log = pluginPath.toPath().resolve(expression + ".log");
        runner.run(
                Map.of("expression", expression, "output", log.toAbsolutePath().toString()),
                pluginPath,
                null,
                "-q",
                "help:evaluate");
        String result;
        try {
            result = Files.readString(log, Charset.defaultCharset()).trim();
            Files.deleteIfExists(log);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public List<String> evaluateList(String expression) throws PomExecutionException {
        Path log = pluginPath.toPath().resolve(expression + ".log");
        runner.run(
                Map.of("expression", expression, "output", log.toAbsolutePath().toString()),
                pluginPath,
                null,
                "-q",
                "help:evaluate");
        List<String> output;
        try {
            output = Files.readAllLines(log, Charset.defaultCharset());
            Files.deleteIfExists(log);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> result = new ArrayList<>();
        for (String line : output) {
            if (!StringUtils.startsWith(line.trim(), "<string>")) {
                continue;
            }
            result.add(line.replace("<string>", "").replace("</string>", "").trim());
        }
        return result;
    }
}
