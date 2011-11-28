package com.khmlabs.synctab.util;

/**
 * Utils to work with strings.
 */
public class StringUtil {

    /**
     * Check if string is empty: either null or length is 0.
     *
     * @param string the string to check.
     * @return true if empty, otherwise false.
     */
    public static boolean isEmpty(String string) {
        return (string == null || string.length() == 0);
    }

    /**
     * Check if string is not empty: neither null nor length is 0.
     *
     * @param string the string to check.
     * @return true if not empty, otherwise false.
     */
    public static boolean isNotEmpty(String string) {
        return (string != null && string.length() > 0);
    }
}
