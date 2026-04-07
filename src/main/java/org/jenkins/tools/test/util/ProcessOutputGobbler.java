package org.jenkins.tools.test.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;

public class ProcessOutputGobbler extends Thread {

    @NonNull
    private final Process p;

    @CheckForNull
    private final File buildLogFile;

    public ProcessOutputGobbler(@NonNull Process p, @Nullable File buildLogFile) {
        this.p = p;
        this.buildLogFile = buildLogFile;
    }

    @Override
    public void run() {
        try (InputStream is = p.getInputStream();
                Reader isr = new InputStreamReader(is, Charset.defaultCharset());
                BufferedReader r = new BufferedReader(isr);
                OutputStream os = buildLogFile == null
                        ? OutputStream.nullOutputStream()
                        : new FileOutputStream(buildLogFile, true);
                Writer osw = new OutputStreamWriter(os, Charset.defaultCharset());
                PrintWriter w = new PrintWriter(osw)) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(line);
                w.println(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
