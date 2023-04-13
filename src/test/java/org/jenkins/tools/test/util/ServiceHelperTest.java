package org.jenkins.tools.test.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestEngine;

class ServiceHelperTest {

    @Test
    void testServiceLocationWithoutExteneralJar() {
        // Use a service that is not from our code as our services will not be found until we are packaged
        List<?> loadServices = ServiceHelper.loadServices(TestEngine.class, Collections.emptySet());
        assertThat(loadServices, not(empty()));
    }

    @Test
    void testServiceLocationWithExternalJar() throws IOException {
        // use javax.annotation.processing.Processor as the serive as the class needs to be in our classpath, but there
        // needs to be some service implementation inside the megwar that is not on our classpath.
        // anotation-indexer has an implemtation of this
        File annotationIndexer = locateAnnotationIndexerJar();

        List<?> baseServices = ServiceHelper.loadServices(Processor.class, Collections.emptySet());
        List<?> extraServices = ServiceHelper.loadServices(Processor.class, Set.of(annotationIndexer));

        assertThat(extraServices.size(), greaterThan(baseServices.size()));
    }

    private static File locateAnnotationIndexerJar() throws IOException {
        // unpacked megawar directory
        Path libDir = Path.of("target", "megawar", "WEB-INF", "lib");
        try (Stream<Path> pathStream = Files.list(libDir)) {
            return pathStream
                    .filter(p -> p.getFileName().toString().startsWith("annotation-indexer-"))
                    .findFirst()
                    .orElseThrow()
                    .toFile();
        }
    }
}
