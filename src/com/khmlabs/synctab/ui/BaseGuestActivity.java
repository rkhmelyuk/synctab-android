package com.khmlabs.synctab.ui;

import android.view.Menu;
import android.view.MenuInflater;

import com.khmlabs.synctab.R;

/**
 * The base activity for guest activities.
 * Guest activity is that shown to unregistered users, like Login or Register.
 */
abstract class BaseGuestActivity extends BaseActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // ---- setup guest activity menu

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.guest_activity, menu);

        return true;
    }

}
