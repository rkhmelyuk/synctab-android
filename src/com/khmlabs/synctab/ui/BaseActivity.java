package com.khmlabs.synctab.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.khmlabs.synctab.R;
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

    /**
     * Here we handle a base menu items selection.
     * Activity implementation should extend with own menu items.
     *
     * Method returns true if menu item selection was handled, and false if not.
     * Activity implementation should check for other options only when this method returns false.
     *
     * @param item the selected menu item.
     * @return true if item was handled, otherwise false.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                new LogoutTask().execute();
                return true;
            case R.id.help:
                // just opens a documentation page
                IntentHelper.browseDocumentation(this);
                return true;
        }
        return false;
    }

    /**
     * Check if user is not authorized yet, and if not than show a login page.
     */
    protected void loginIfNotAuthenticated() {
        SyncTabApplication app = getSyncTabApplication();
        if (!app.isAuthenticated()) {
            showLogin();
        }
    }

    /**
     * Show a login activity.
     */
    protected void showLogin() {
        final Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(loginIntent, 1);
    }

    protected SyncTabApplication getSyncTabApplication() {
        return (SyncTabApplication) getApplication();
    }

    /**
     * Logout current user. This operation may make a remote call, so this is an AsyncTask.
     * Also shows the progress dialog, so user know the status of operation.
     * After logout shows a login screen.
     */
    private class LogoutTask extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final String message = getResources().getString(R.string.logout_progress);
            progress = ProgressDialog.show(BaseActivity.this, null, message, true, false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            progress.dismiss();
            showLogin();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                getSyncTabApplication().logout();
            }
            catch (Exception e) {
                Log.e("LogoutTask", "Failed to logout", e);
            }
            return null;
        }
    }
}
