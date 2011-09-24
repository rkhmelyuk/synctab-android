package com.khmlabs.synctab.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncTabService extends Service {

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        // TODO - if there are tabs to add, add
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
