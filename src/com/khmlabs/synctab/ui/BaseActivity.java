package com.khmlabs.synctab.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.khmlabs.synctab.SyncTabApplication;

abstract class BaseActivity extends Activity {

    private static final int REQUEST_LOGIN = 1;

    TitleBarHelper titlebarHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        titlebarHelper = new TitleBarHelper(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        titlebarHelper.setup();
    }

    protected void onResume() {
        super.onResume();

        loginIfNotAuthenticated();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOGIN) {
            if (resultCode != RESULT_OK) {
                showLogin();
            }
        }
    }

    protected void showLogin() {
        final Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(loginIntent, 1);
    }

    protected void loginIfNotAuthenticated() {
        SyncTabApplication app = getSyncTabApplication();
        if (!app.isAuthenticated()) {
            showLogin();
        }
    }

    protected SyncTabApplication getSyncTabApplication() {
        return (SyncTabApplication) getApplication();
    }
}
