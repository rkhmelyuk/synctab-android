package com.khmlabs.synctab;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        final EditText emailInput = (EditText) findViewById(R.id.email);
        final EditText passwordInput = (EditText) findViewById(R.id.password);

        Button loginButton = (Button) findViewById(R.id.login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // TODO - getText(), getString()
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (email.length() != 0 && password.length() != 0) {
                    new AuthorizeTask().execute(email, password);
                }
                else {
                    Toast.makeText(LoginActivity.this, R.string.email_password_required, 3000).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SyncTabApplication app = (SyncTabApplication) getApplication();
        if (app.isAuthenticated()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
    }

    private class AuthorizeTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                final SyncTabRemoteService service = app.getSyncTabRemoteService();

                return service.authenticate(strings[0], strings[1]);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to authenticate");
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {
            super.onPostExecute(status);

            if (status) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
            else {
                Toast.makeText(LoginActivity.this, R.string.error_authenticate, 5000).show();
            }
        }
    }
}