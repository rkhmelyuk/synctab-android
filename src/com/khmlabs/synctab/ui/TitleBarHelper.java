package com.khmlabs.synctab.ui;

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

/**
 * Title bar is better known as ActionBar.
 * Used to show an application title and action buttons.
 */
class TitleBarHelper {

    /**
     * The activity where to show a title bar.
     */
    final BaseActivity activity;

    /**
     * The id of the home button.
     */
    private static final int HOME_BUTTON_ID = 0x7f0b9999;
    /**
     * The id of the refresh button.
     */
    private static final int REFRESH_BUTTON_ID = 0x7f0b9998;

    public TitleBarHelper(BaseActivity activity) {
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

        if (activity instanceof MainActivity) {
            // Add refresh button only to MainActivity
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_refresh, REFRESH_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            refreshTabs();
                        }
                    });
        }
        else {
            // Add home button, but not for MainActivity (which is a Home itself).
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_home, HOME_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            goHome();
                        }
                    });
        }
    }

    private boolean activityIsSupported() {
        if (activity instanceof BaseGuestActivity) {
            // not supported from guest activities
            return false;
        }
        else if (!activity.getSyncTabApplication().isAuthenticated()) {
            // not supported for guest users
            return false;
        }

        return true;
    }

    private void refreshTabs() {
        MainActivity mainActivity = (MainActivity) activity;
        mainActivity.refreshSharedTabs();
    }

    private void goHome() {
        if (activity instanceof MainActivity) {
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
