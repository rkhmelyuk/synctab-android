package com.khmlabs.synctab.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.util.IntentHelper;

abstract class BaseActivity extends Activity {

    /** The activity titlebar helper. */
    TitleBarHelper titlebarHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        titlebarHelper = new TitleBarHelper(this);
    }

    protected void onResume() {
        super.onResume();

        // add any buttons to title bar
        titlebarHelper.setup();
    }
    /**
     * Here we handle a base menu items selection.
     * Activity implementation should extend with own menu items.
     * <p/>
     * Method returns true if menu item selection was handled, and false if not.
     * Activity implementation should check for other options only when this method returns false.
     *
     * @param item the selected menu item.
     * @return true if item was handled, otherwise false.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about: {
                // shows an about activity
                IntentHelper.showAboutActivity(this);
                return true;
            }
            case R.id.help: {
                // just opens a documentation page
                IntentHelper.browseDocumentation(this);
                return true;
            }
        }
        return false;
    }

    /**
     * Get SyncTab Application instance.
     * @return the application instance.
     */
    protected SyncTabApplication getSyncTabApplication() {
        return (SyncTabApplication) getApplication();
    }
}
