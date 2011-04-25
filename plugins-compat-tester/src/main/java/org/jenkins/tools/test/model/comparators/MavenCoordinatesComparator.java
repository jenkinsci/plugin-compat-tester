package org.jenkins.tools.test.model.comparators;

import org.jenkins.tools.test.model.MavenCoordinates;

import java.util.Comparator;

public class MavenCoordinatesComparator implements Comparator<MavenCoordinates> {
    public int compare(MavenCoordinates o1, MavenCoordinates o2) {
        return o1.version.compareTo(o2.version);
    }
}
