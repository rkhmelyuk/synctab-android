package com.khmlabs.synctab.util;

import java.io.*;

public class IOUtil {

    private static final int BUFFER_SIZE = 8192;

    public static String toString(InputStream stream, int length) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder(length > 0 ? length : 32);

        String line;
        while ((line = reader.readLine())  != null) {
            builder.append(line);
        }

        return builder.toString();
    }

    public static void writeToOutput(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];

        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException e) {
                // ignore
            }
        }
    }
}