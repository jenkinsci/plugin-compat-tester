package org.jenkins.tools.test.picocli;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/** Converter that converts to a File that must exist (either as a directory or a file) */
public class ExistingFileTypeConverter implements ITypeConverter<File> {

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "by design, we are converting an argument from the CLI")
    @Override
    public File convert(String value) throws Exception {
        File f = new File(value);
        if (!f.exists()) {
            throw new TypeConversionException("Specified file " + value + " does not exist");
        }
        return f;
    }
}
