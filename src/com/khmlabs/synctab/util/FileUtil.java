package com.khmlabs.synctab.util;

import java.io.File;

/**
 * The utils to work with files and directories.
 */
public class FileUtil {


    /**
     * Delete the directory with it's content.
     *
     * @param directory the directory to delete.
     */
    public static void deleteDirectory(File directory) {
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

    /**
     * Calculate the total directory size.
     * Method can stop after reaching <code>max</code> value,
     * but if max <= 0, it will continue till get the total directory size.
     *
     * @param directory the directory to get size of.
     * @param max the max directory size that is enough to stop calculating.
     * @return the total directory size or max.
     */
    public static long calculateDirectorySize(File directory, long max) {
        final File[] files = directory.listFiles();
        long total = 0;
        for (File each : files) {
            if (each.isFile()) {
                total += each.length();
            }
            else if (each.isDirectory()) {
                total += calculateDirectorySize(each, max);
            }

            if (max > 0 && total > max) {
                return total;
            }
        }
        return total;
    }
}
