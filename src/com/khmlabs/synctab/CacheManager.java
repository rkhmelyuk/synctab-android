package com.khmlabs.synctab;

import android.content.Context;
import android.util.Log;

import com.khmlabs.synctab.util.FileUtil;
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
        FileUtil.deleteDirectory(cacheDir);
    }

    public boolean isNeedCleanup() {
        final File cacheDir = context.getCacheDir();
        return FileUtil.calculateDirectorySize(cacheDir, CACHE_MAXSIZE) < CACHE_MAXSIZE;
    }

    public boolean store(String name, InputStream content) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(name);
        final File file = new File(cacheDir, fileName);

        return writeFileContent(file, content);
    }

    public InputStream read(String name) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(name);
        final File file = new File(cacheDir, fileName);

        return readFileContent(file);
    }

    private static String convertToFileName(String name) {
        name = name.replaceAll("[^a-zA-Z0-9_\\-]*", "");
        if (name.length() > 256) {
            name = name.substring(0, 255);
        }
        return name;
    }

    private static boolean writeFileContent(File file, InputStream content) {
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

    private static InputStream readFileContent(File file) {
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

}
