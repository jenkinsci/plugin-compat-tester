package org.jenkins.tools.test.model.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

/**
 * @author fcamblor
 */
public class IOUtilsTest {

    @Test
    public void shouldCompressUncompressBeIdemPotent() throws IOException, ClassNotFoundException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("test.log");
        String initialContent = IOUtils.streamToString(is);

        String encodedContent = IOUtils.gzipString(initialContent);
        assertTrue(initialContent.length() > encodedContent.length());

        String decodedContent = IOUtils.gunzipString(encodedContent);
        assertThat(decodedContent, is(equalTo(initialContent)));
    }
}
