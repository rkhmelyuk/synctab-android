package com.khmlabs.synctab.util;

import android.content.ContextWrapper;
import android.content.pm.PackageInfo;

import com.khmlabs.synctab.R;

/**
 * Helper for Android application.
 */
public class AppHelper {

    /**
     * Gets the version name (not a code) ie 1.1, 1.2.
     *
     * @param ctx the context wrapper.
     * @return the version name.
     */
    public static String getVersionName(ContextWrapper ctx) {
        try {
            String pkgName = ctx.getPackageName();
            PackageInfo info = ctx.getPackageManager().getPackageInfo(pkgName, 0);

            return info.versionName;
        }
        catch (Exception e) {
            return ctx.getResources().getString(R.string.unknown);
        }
    }
}
