package com.khmlabs.synctab.util;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UrlUtil {

    private static final String TAG = "UrlUtil";

    public static String prepareReadableUrl(String url) {
        url = url.replaceFirst("[a-z]+://", "");
        url = url.replaceAll("\\?.*", "");
        url = url.replaceAll("#.*", "");

        try {
            url = URLDecoder.decode(url, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error to decode url " + url);
        }

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    public static String shortenizeUrl(String url, int maxlength) {
        if (url.length() > maxlength) {
            int start = maxlength - 18;
            url = url.substring(0, start) + "..." + url.substring(url.length() - 15);
        }

        return url;
    }
}
