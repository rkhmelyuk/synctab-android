package com.khmlabs.synctab.ui;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.util.IntentHelper;

/**
 * The base activity for guest activities.
 * Guest activity is that shown to unregistered users, like Login or Register.
 */
public abstract class GuestActivity extends Activity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // ---- setup guest activity menu

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.guest_activity, menu);

        return true;
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
            case R.id.help:
                IntentHelper.browseDocumentation(this);
                return true;
        }
        return false;
    }
}
