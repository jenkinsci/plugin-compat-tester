package org.jenkins.tools.test.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

public class StreamGobbler extends Thread {

    @NonNull private final InputStream is;

    private final StringBuilder output = new StringBuilder();

    public StreamGobbler(@NonNull InputStream is) {
        this.is = is;
    }

    public String getOutput() {
        return output.toString();
    }

    @Override
    public void run() {
        try (Reader r = new InputStreamReader(is, Charset.defaultCharset()); BufferedReader br = new BufferedReader(r)) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
                output.append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
