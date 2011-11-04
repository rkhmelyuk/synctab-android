package com.khmlabs.synctab;

import android.content.Context;
import android.util.Log;

import com.khmlabs.synctab.util.FileUtil;
import com.khmlabs.synctab.util.IOUtil;

import java.io.*;

/**
 * This manager handle the work with file cache.
 */
public class FileCacheManager {

    private static final String TAG = "FileCacheManager";
    private static final int CACHE_MAXSIZE = 1024 * 1024;

    private final Context context;

    public FileCacheManager(Context context) {
        this.context = context;
    }

    /**
     * Remove everything from the cache.
     */
    public void clean() {
        final File cacheDir = context.getCacheDir();
        FileUtil.deleteDirectory(cacheDir);
    }

    /**
     * Check if need to cleanup the cache.
     * Usually cache need to be cleaned up when it's size is over specified margin.
     *
     * @return true if cache is need to cleanup.
     */
    public boolean isNeedCleanup() {
        final File cacheDir = context.getCacheDir();
        return FileUtil.calculateDirectorySize(cacheDir, CACHE_MAXSIZE) < CACHE_MAXSIZE;
    }

    /**
     * Check if specified key is already stored in the cache.
     *
     * @param key the cache key.
     * @return true if stored, otherwise false.
     */
    public boolean containsKey(String key) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(key);
        final File file = new File(cacheDir, fileName);

        return file.exists();
    }

    /**
     * Store the file by specified cache key.
     *
     * Cache key is converted into file name. While this procedure different cache keys can point the same file.
     * Need to try to used letter and numbers where possible.
     *
     * @param key the cache key.
     * @param content the content to save in cache.
     * @return true if was saved successfully.
     */
    public boolean store(String key, InputStream content) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(key);
        final File file = new File(cacheDir, fileName);

        return writeFileContent(file, content);
    }

    /**
     * Read the file by specified cache key.
     *
     * Cache key is converted into file name. While this procedure different cache keys can point the same file.
     * Need to try to used letter and numbers where possible.
     *
     * @param key the cache key.
     * @return the stream from found file or null if nothing was found by cache key.
     */
    public InputStream read(String key) {
        final File cacheDir = context.getCacheDir();
        final String fileName = convertToFileName(key);
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

}
