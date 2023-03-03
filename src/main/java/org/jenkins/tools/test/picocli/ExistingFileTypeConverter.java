package org.jenkins.tools.test.picocli;

import java.io.File;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/** Converter that converts to a File that must exist (either as a directory or a file) */
public class ExistingFileTypeConverter implements ITypeConverter<File> {

    @Override
    public File convert(String value) throws Exception {
        File f = new File(value);
        if (!f.exists()) {
            throw new TypeConversionException("Specified file " + value + " does not exist");
        }
        return f;
    }
}
