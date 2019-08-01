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
package org.jenkins.tools.test.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * POJO containing important data residing in plugin's pom
 * @author Frederic Camblor
 */
public class PomData {
    public final String artifactId;

    @Nonnull
    private final String packaging;

    @CheckForNull
    public final MavenCoordinates parent;
    private String connectionUrl;
    private String scmTag;
    private List<String> warningMessages = new ArrayList<String>();

    public PomData(String artifactId, @CheckForNull String packaging, String connectionUrl, String scmTag, @CheckForNull MavenCoordinates parent){
        this.artifactId = artifactId;
        this.packaging = packaging != null ? packaging : "jar";
        this.setConnectionUrl(connectionUrl);
        this.scmTag = scmTag;
        this.parent = parent;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    @Nonnull
    public String getPackaging() {
        return packaging;
    }

    public String getScmTag() {
        return scmTag;
    }

    public boolean isPluginPOM() {
        if (parent != null) {
            return parent.matches("org.jenkins-ci.plugins", "plugin");
        } else { // Interpolate by packaging
            return "hpi".equalsIgnoreCase(packaging);
        }
    }
}
