package com.khmlabs.synctab.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.service.SyncTabService;

public class NetworkReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final SyncTabApplication app = (SyncTabApplication) context.getApplicationContext();
        final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        final Intent serviceIntent = new Intent(context, SyncTabService.class);

        if (networkInfo != null && networkInfo.isConnected()) {
            app.setOnLine(true);
            context.startService(serviceIntent);
        }
        else {
            app.setOnLine(false);
            context.stopService(serviceIntent);
        }
    }
}
