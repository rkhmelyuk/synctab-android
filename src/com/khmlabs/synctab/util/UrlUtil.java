package com.khmlabs.synctab.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtil {

    private static final String TAG = "UrlUtil";
    private static final Pattern LINK_SPLIT = Pattern.compile("^([^/]+)(.*)");
    private static final int URL_PATH_COLOR = 0xff999999;

    public static String prepareReadableUrl(String url) {
        url = decodeLink(url);
        url = url.replaceFirst("[a-z]+://", "");
        url = url.replaceAll("[?#].*", "");
        url = url.replaceAll("(.*)/+$", "$1");

        return url;
    }

    public static String shortenizeUrl(String url, int maxlength) {
        if (url.length() > maxlength) {
            int start = maxlength - 18;
            url = url.substring(0, start) + "..." + url.substring(url.length() - 15);
        }

        return url;
    }

    public static CharSequence colorizeUrl(String url) {
        final Matcher matcher = LINK_SPLIT.matcher(url);
        if (matcher.find()) {
            final int pathStart = matcher.start(2);
            final int pathEnd = matcher.end(2);

            if (pathStart != pathEnd) {
                final Spannable spannable = new SpannableString(url);
                spannable.setSpan(new ForegroundColorSpan(URL_PATH_COLOR),
                        pathStart, pathEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return spannable;
            }
        }

        return url;
    }

    public static String decodeLink(String url) {
        try {
            url = URLDecoder.decode(url, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error to decode url " + url);
        }
        return url;
    }
}
