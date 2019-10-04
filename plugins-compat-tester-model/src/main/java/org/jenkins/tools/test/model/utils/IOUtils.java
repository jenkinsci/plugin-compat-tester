package org.jenkins.tools.test.model.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;

/**
 * @author fcamblor
 */
public class IOUtils {

    public static String streamToString(InputStream is) throws IOException {
        StringWriter sw = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            int n;
            while((n = r.read(buffer)) != -1){
                sw.write(buffer, 0, n);
            }
        }finally{
            is.close();
        }
        return sw.toString();
    }

    public static String gunzipString(String strToUncompress) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.decodeBase64(strToUncompress.getBytes(StandardCharsets.UTF_8)));
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ObjectInputStream ois = new ObjectInputStream(gzipInputStream);

        String unzippedString = (String)ois.readObject();

        ois.close();
        gzipInputStream.close();
        byteArrayInputStream.close();

        return unzippedString;
    }

    public static String gzipString(String strToCompress) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(strToCompress.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
        ObjectOutputStream oos = new ObjectOutputStream(gzipOutputStream);

        oos.writeObject(strToCompress);
        oos.flush();

        oos.close();
        gzipOutputStream.close();
        bais.close();
        baos.close();

        return new String(Base64.encodeBase64(baos.toByteArray()), StandardCharsets.UTF_8);
    }
}
