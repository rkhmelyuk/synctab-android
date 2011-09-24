package com.khmlabs.synctab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IOUtil {

    public static String toString(InputStream stream, int length) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder(length > 0 ? length : 32);

        String line;
        while ((line = reader.readLine())  != null) {
            builder.append(line);
        }

        return builder.toString();
    }
}