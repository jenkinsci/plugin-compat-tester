/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkins.tools.test.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Utility class to fork print streams for logging
 * @author Frederic Camblor
 */
public class SystemIOLoggerFilter extends PrintStream {

    private File currentPSFile;

    public SystemIOLoggerFilter(File currentPSFile) throws FileNotFoundException {
        super(currentPSFile);
        this.currentPSFile = currentPSFile;
    }

    public File getCurrentPSFile() {
        return currentPSFile;
    }

    public static class SystemIOWrapper extends PrintStream {
        private SystemIOLoggerFilter loggerFilter;
        private PrintStream systemIO;

        public SystemIOWrapper(SystemIOLoggerFilter loggerFilter, PrintStream systemIO) throws FileNotFoundException {
            // Should be unnecessary but PrintStream doesn't have any default constructor !
            super(new FileOutputStream(loggerFilter.getCurrentPSFile(), true));
            this.loggerFilter = loggerFilter;
            this.systemIO = systemIO;
        }

        public void write(int b) {
            loggerFilter.write(b);
            systemIO.write(b);
        }

        public void write(byte[] b, int off, int len) {
            loggerFilter.write(b, off, len);
            systemIO.write(b, off, len);
        }

        public void flush() {
            loggerFilter.flush();
            systemIO.flush();
        }

        public void close() {
            loggerFilter.close();
            systemIO.close();
        }

        public PrintStream format(Locale l, String format, Object... args) {
            loggerFilter.format(l, format, args);
            return systemIO.format(l, format, args);
        }
    }
}
