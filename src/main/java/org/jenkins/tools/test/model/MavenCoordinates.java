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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.jenkins.tools.test.model.comparators.VersionComparator;

/**
 * Class representing Maven GAV
 *
 * @author Frederic Camblor
 */
public class MavenCoordinates implements Comparable<MavenCoordinates> {
    public final String groupId;
    public final String artifactId;
    public final String version;
    // No classifier/type for the moment...

    /**
     * Constructor.
     *
     * @throws IllegalArgumentException one of the parameters is invalid.
     */
    public MavenCoordinates(@NonNull String groupId, @NonNull String artifactId, @NonNull String version){
        this.groupId = verifyInput( groupId, artifactId, version,"groupId", groupId);
        this.artifactId = verifyInput( groupId, artifactId, version,"artifactId", artifactId);
        this.version = verifyInput( groupId, artifactId, version,"version", version);
    }

    private static String verifyInput(String groupId, String artifactId, String version,
                                      String fieldName, String value) throws IllegalArgumentException {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    String.format("Invalid parameter passed for %s:%s:%s: Field %s; %s",
                            groupId, artifactId, version, fieldName, value));
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MavenCoordinates c2 = (MavenCoordinates)o;
        return Objects.equals(groupId, c2.groupId) && Objects.equals(artifactId, c2.artifactId) && Objects.equals(version, c2.version);
    }

    @Override
    public int hashCode(){
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString(){
        return "MavenCoordinates[groupId="+groupId+", artifactId="+artifactId+", version="+version+"]";
    }

    public String toGAV(){
        return groupId+":"+artifactId+":"+version;
    }

    public static MavenCoordinates fromGAV(String gav){
        String[] chunks = gav.split(":");
        return new MavenCoordinates(chunks[0], chunks[1], chunks[2]);
    }

    @Override
    public int compareTo(MavenCoordinates o) {
        if((groupId+":"+artifactId).equals(o.groupId+":"+o.artifactId)){
            return compareVersionTo(o.version);
        } else {
            return (groupId+":"+artifactId).compareTo(o.groupId+":"+o.artifactId);
        }
    }

    public boolean matches(String groupId, String artifactId) {
        return this.groupId.equals(groupId) && this.artifactId.equals(artifactId);
    }

    public int compareVersionTo(String version) {
        return new VersionComparator().compare(this.version, version);
    }
}
