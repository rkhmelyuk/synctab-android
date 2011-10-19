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

class TitleBarHelper {

    final Activity activity;

    private static final int HOME_BUTTON_ID = 0x7f0b9999;
    private static final int REFRESH_BUTTON_ID = 0x7f0b9998;

    public TitleBarHelper(Activity activity) {
        this.activity = activity;
    }

    public void setup() {
        final LinearLayout layout = getButtonsLayout();

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                (int) activity.getResources().getDimension(R.dimen.titlebar_button_width),
                (int) activity.getResources().getDimension(R.dimen.titlebar_button_height), 1);

        if (activity instanceof MainActivity) {
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_refresh, REFRESH_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            refreshTabs();
                        }
                    });
        }
        else {
            addSeparator(layout);
            addButton(layout, layoutParams, R.drawable.ic_title_home, HOME_BUTTON_ID,
                    new View.OnClickListener() {
                        public void onClick(View view) {
                            goHome();
                        }
                    });
        }
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
