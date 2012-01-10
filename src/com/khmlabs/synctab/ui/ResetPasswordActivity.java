package com.khmlabs.synctab.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.khmlabs.synctab.R;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabFacade;

public class ResetPasswordActivity extends BaseGuestActivity {

    private static final String TAG = "ResetPasswordActivity";

    EditText emailInput;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        emailInput = (EditText) findViewById(R.id.email);

        emailInput.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    resetPassword();
                    return true;
                }
                return false;
            }
        });

        final Button resetPasswordButton = (Button) findViewById(R.id.reset_password);
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                resetPassword();
            }
        });
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        if (email.length() != 0) {
            new ResetPasswordTask().execute(email);
        }
        else {
            Toast.makeText(this, R.string.email_required, 3000).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SyncTabApplication app = (SyncTabApplication) getApplication();
        if (app.isAuthenticated()) {
            startActivity(new Intent(ResetPasswordActivity.this, MainActivity.class));
            finish();
        }
    }

    private class ResetPasswordTask extends AsyncTask<String, String, Integer> {

        static final int RESULT_SUCCESS = 0;
        static final int RESULT_FAILED = 1;
        static final int RESULT_OFFLINE = 2;
        static final int RESULT_ERROR = 3;

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final String message = getResources().getString(R.string.please_wait);
            progress = ProgressDialog.show(ResetPasswordActivity.this, null, message, true, false);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                if (app.isOnLine()) {
                    final SyncTabFacade facade = app.getFacade();
                    final boolean result = facade.resetPassword(strings[0]);

                    return result ? RESULT_SUCCESS : RESULT_FAILED;
                }
                return RESULT_OFFLINE;
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to reset a password");
                return RESULT_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);

            progress.dismiss();

            if (status == RESULT_SUCCESS) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ResetPasswordActivity.this);
                builder.setTitle(getResources().getString(R.string.done));
                builder.setCancelable(false);
                builder.setMessage(R.string.reset_password_successfully);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent loginIntent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(loginIntent);
                    }
                });

                builder.create().show();
            }
            else if (status == RESULT_FAILED) {
                Toast.makeText(ResetPasswordActivity.this, R.string.failed_reset_password, 5000).show();
            }
            else if (status == RESULT_ERROR) {
                Toast.makeText(ResetPasswordActivity.this, R.string.error_reset_password, 5000).show();
            }
            else if (status == RESULT_OFFLINE) {
                Toast.makeText(ResetPasswordActivity.this, R.string.no_connection, 5000).show();
            }
        }
    }
}