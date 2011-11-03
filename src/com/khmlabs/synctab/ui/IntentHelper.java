package com.khmlabs.synctab.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.khmlabs.synctab.R;

public class IntentHelper {

    /**
     * Opens a documentation.
     *
     * @param ctx the current context.
     */
    public static void browseDocumentation(Context ctx) {
        browseLink(ctx, ctx.getResources().getString(R.string.help_url));
    }

    /**
     * Opens a link in the browser.
     *
     * @param ctx the current context.
     * @param link the link to open in the browser.
     */
    public static void browseLink(Context ctx, String link) {
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }
}
