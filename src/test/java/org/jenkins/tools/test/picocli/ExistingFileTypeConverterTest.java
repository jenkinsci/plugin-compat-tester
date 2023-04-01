package org.jenkins.tools.test.picocli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine.TypeConversionException;

class ExistingFileTypeConverterTest {

    @Test
    void testValidFile(@TempDir File f) throws Exception {
        ExistingFileTypeConverter converter = new ExistingFileTypeConverter();
        File converted = converter.convert(f.getPath());
        assertThat(converted, is(f));
    }

    @Test
    void testMissingFile(@TempDir File f) throws Exception {
        ExistingFileTypeConverter converter = new ExistingFileTypeConverter();
        TypeConversionException tce =
                assertThrows(TypeConversionException.class, () -> converter.convert(new File(f, "whatever").getPath()));
        assertThat(tce.getMessage(), containsString("whatever"));
    }
}
