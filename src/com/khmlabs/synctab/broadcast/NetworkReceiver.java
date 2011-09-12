package com.khmlabs.synctab.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.service.SyncTabService;

public class NetworkReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        boolean isNetworkDown = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        SyncTabApplication app = (SyncTabApplication) context.getApplicationContext();
        app.setOnLine(!isNetworkDown);

        if (isNetworkDown) {
            context.stopService(new Intent(context, SyncTabService.class));
        }
        else {
            context.startService(new Intent(context, SyncTabService.class));
        }
    }
}
