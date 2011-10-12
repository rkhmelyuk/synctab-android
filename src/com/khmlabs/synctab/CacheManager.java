package com.khmlabs.synctab;

import android.content.Context;
import android.util.Log;
import com.khmlabs.synctab.util.IOUtil;

import java.io.*;

public class CacheManager {

    private static final String TAG = "CacheManager";
    private static final int CACHE_MAXSIZE = 1024 * 1024;

    private final Context context;

    public CacheManager(Context context) {
        this.context = context;
    }

    public void clean() {
        final File cacheDir = context.getCacheDir();
        deleteDirectory(cacheDir);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    private void deleteDirectory(File directory) {
        final File[] files = directory.listFiles();
        for (File each : files) {
            if (each.isFile()) {
                each.delete();
            }
            else if (each.isDirectory()) {
                deleteDirectory(each);
            }
        }
    }

    public boolean isNeedCleanup() {
        final File cacheDir = context.getCacheDir();
        final File[] files = cacheDir.listFiles();

        long total = 0;
        for (File each : files) {
            if (each.isFile()) {
                total += each.length();
                if (total > CACHE_MAXSIZE) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean store(String name, InputStream content) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(name);
        final File file = new File(cacheDir, fileName);

        if (file.exists()) {
            return true;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            IOUtil.writeToOutput(content, out);

            return true;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to store file in cache.", e);
        }
        finally {
            IOUtil.close(out);
        }

        return false;
    }

    public InputStream read(String name) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(name);
        final File file = new File(cacheDir, fileName);

        if (!file.exists()) {
            return null;
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtil.writeToOutput(in, out);

            return new ByteArrayInputStream(out.toByteArray());
        }
        catch (Exception e) {
            Log.e(TAG, "Error to read file in cache.", e);
        }
        finally {
            IOUtil.close(in);
        }

        return null;
    }

    private static String convertToFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]*", "");
    }

}
