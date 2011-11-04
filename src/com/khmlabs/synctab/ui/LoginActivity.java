package com.khmlabs.synctab.ui;

import android.app.ProgressDialog;
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
import com.khmlabs.synctab.SyncTabService;

public class LoginActivity extends GuestActivity {

    private static final String TAG = "LoginActivity";

    EditText emailInput;
    EditText passwordInput;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = (EditText) findViewById(R.id.email);

        passwordInput = (EditText) findViewById(R.id.password);
        passwordInput.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    login();
                    return true;
                }
                return false;
            }
        });

        final Button loginButton = (Button) findViewById(R.id.login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                login();
            }
        });

        final Button registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this, RegistrationActivity.class);
                registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(registerIntent);
                finish();
            }
        });
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.length() != 0 && password.length() != 0) {
            new AuthorizeTask().execute(email, password);
        }
        else {
            Toast.makeText(this, R.string.email_password_required, 3000).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SyncTabApplication app = (SyncTabApplication) getApplication();
        if (app.isAuthenticated()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private class AuthorizeTask extends AsyncTask<String, String, Integer> {

        static final int RESULT_SUCCESS = 0;
        static final int RESULT_FAILED = 1;
        static final int RESULT_OFFLINE = 2;
        static final int RESULT_ERROR = 3;

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final String message = getResources().getString(R.string.authentication);
            progress = ProgressDialog.show(LoginActivity.this, null, message, true, false);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                if (app.isOnLine()) {
                    final SyncTabService service = app.getSyncTabService();
                    final boolean result = service.authenticate(strings[0], strings[1]);

                    return result ? RESULT_SUCCESS : RESULT_FAILED;
                }
                return RESULT_OFFLINE;
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to authenticate");
                return RESULT_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);

            progress.dismiss();

            if (status == RESULT_SUCCESS) {
                //if (getParent() != null) {
                setResult(RESULT_OK);
                finish();
                //}
                /*else {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }*/
            }
            else if (status == RESULT_FAILED) {
                Toast.makeText(LoginActivity.this, R.string.failed_authenticate, 5000).show();
            }
            else if (status == RESULT_ERROR) {
                Toast.makeText(LoginActivity.this, R.string.error_authenticate, 5000).show();
            }
            else if (status == RESULT_OFFLINE) {
                Toast.makeText(LoginActivity.this, R.string.no_connection, 5000).show();
            }
        }
    }
}