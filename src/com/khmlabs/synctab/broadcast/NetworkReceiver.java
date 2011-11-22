package com.khmlabs.synctab.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.service.RefreshService;
import com.khmlabs.synctab.service.SyncService;

/**
 * Starts the {@link SyncService} and {@link RefreshService} when online and stop when offline.
 *
 * @author Ruslan Khmelyuk
 */
public class NetworkReceiver extends BroadcastReceiver {

    public void onReceive(Context ctx, Intent intent) {
        final SyncTabApplication app = (SyncTabApplication) ctx.getApplicationContext();
        final ConnectivityManager manager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        final Intent syncIntent = new Intent(ctx, SyncService.class);
        final Intent refreshIntent = new Intent(ctx, RefreshService.class);

        if (networkInfo != null && networkInfo.isConnected()) {
            app.setOnLine(true);
            ctx.startService(syncIntent);
            ctx.startService(refreshIntent);
        }
        else {
            app.setOnLine(false);
            ctx.stopService(syncIntent);
            ctx.stopService(refreshIntent);
        }
    }
}
