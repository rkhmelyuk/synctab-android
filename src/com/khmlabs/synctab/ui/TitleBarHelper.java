package com.khmlabs.synctab.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;

/**
 * Title bar is better known as ActionBar.
 * Used to show an application title and action buttons.
 */
class TitleBarHelper {

    /**
     * The activity where to show a title bar.
     */
    final Activity activity;

    /** The id of the home button. */
    private static final int HOME_BUTTON_ID = 0x7f0b9999;

    /** The id of the refresh button. */
    private static final int REFRESH_BUTTON_ID = 0x7f0b9998;

    /** The id of the add button. */
    private static final int ADD_BUTTON_ID = 0x7f0b9997;

    public TitleBarHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Setup the title bar: adds button panel with required buttons.
     * This action has no effect for guest activities.
     */
    public void setup() {
        // titlebar isn't supported by all activities
        if (!activityIsSupported()) {
            return;
        }

        final LinearLayout layout = getButtonsLayout();

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                (int) activity.getResources().getDimension(R.dimen.titlebar_button_width),
                (int) activity.getResources().getDimension(R.dimen.titlebar_button_height), 1);

        if (isHomeVisible()) {
            // Add home button, but not for MainActivity (which is a Home itself).
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_home, HOME_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            goHome();
                        }
                    });
        }
        if (activity instanceof AddSupport) {
            // Add button only if supported
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_add, ADD_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            add();
                        }
                    });
        }
        if (activity instanceof RefreshSupport) {
            // Add refresh button only if supported
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_refresh, REFRESH_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            refresh();
                        }
                    });
        }
    }

    /**
     * Whether home button is visible.
     * @return true if visible and supported.
     */
    private boolean isHomeVisible() {
        return !(activity instanceof MainActivity);
    }

    private boolean activityIsSupported() {
        if (activity instanceof BaseGuestActivity) {
            // not supported from guest activities
            return false;
        }

        // not supported for guest users
        SyncTabApplication app = (SyncTabApplication) activity.getApplication();
        return app.isAuthenticated();
    }

    private void refresh() {
        if (activity instanceof RefreshSupport) {
            ((RefreshSupport) activity).refresh();
        }
    }

    private void add() {
        if (activity instanceof AddSupport) {
            ((AddSupport) activity).add();
        }
    }

    private void goHome() {
        if (!isHomeVisible()) {
            return;
        }

        final Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private LinearLayout getButtonsLayout() {
        final ViewGroup titlebar = (ViewGroup) activity.findViewById(R.id.titlebar);
        final RelativeLayout.LayoutParams titlebarParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.FILL_PARENT);
        titlebarParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        titlebarParams.addRule(RelativeLayout.ALIGN_BASELINE);

        final LinearLayout layout = new LinearLayout(activity);
        layout.setLayoutParams(titlebarParams);

        titlebar.addView(layout);

        return layout;
    }

    private void addButton(LinearLayout layout, LinearLayout.LayoutParams layoutParams,
                           int icon, int id, View.OnClickListener action) {
        final ImageButton button = new ImageButton(activity);
        button.setId(id);
        button.setLayoutParams(layoutParams);
        button.setImageResource(icon);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setBackgroundResource(R.drawable.titlebar_button);

        final Resources res = activity.getResources();
        button.setPadding(
                (int) res.getDimension(R.dimen.titlebar_button_padding_left),
                (int) res.getDimension(R.dimen.titlebar_button_padding_top),
                (int) res.getDimension(R.dimen.titlebar_button_padding_right),
                (int) res.getDimension(R.dimen.titlebar_button_padding_bottom));

        button.setOnClickListener(action);

        layout.addView(button);
    }

    private void addSeparator(LinearLayout layout) {
        final ImageView separator = new ImageView(activity);
        separator.setLayoutParams(new LinearLayout.LayoutParams(2, ViewGroup.LayoutParams.FILL_PARENT));
        separator.setImageResource(R.drawable.titlebar_separator);
        separator.setScaleType(ImageView.ScaleType.FIT_XY);

        layout.addView(separator);
    }

    /**
     * If list of shared tabs is refreshing (<code>refreshing = true</code>), then replace
     * Refresh button in the title bar with an animation.
     * <p/>
     * If list isn't refreshing (<code>refreshing = false</code>), then stop animation,
     * and show a Refresh button back.
     *
     * @param refreshing the boolean flag if refreshing.
     */
    public void setRefreshing(boolean refreshing) {
        final ImageButton button = (ImageButton) activity.findViewById(REFRESH_BUTTON_ID);

        if (refreshing) {
            button.setClickable(false);
            button.setImageResource(R.drawable.ic_title_refreshing);
            ((AnimationDrawable) button.getDrawable()).start();
        }
        else {
            ((AnimationDrawable) button.getDrawable()).stop();
            button.setImageResource(R.drawable.ic_title_refresh);
            button.setClickable(true);
        }
    }
}
