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

public class RegistrationActivity extends Activity {

    private static final String TAG = "RegistrationActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        final EditText emailInput = (EditText) findViewById(R.id.email);
        final EditText passwordInput = (EditText) findViewById(R.id.password);

        Button registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (email.length() != 0 && password.length() != 0) {
                    new RegisterTask().execute(email, password);
                }
                else {
                    Toast.makeText(RegistrationActivity.this, R.string.email_password_required, 3000).show();
                }
            }
        });
    }

    private class RegisterTask extends AsyncTask<String, String, Integer> {

        static final int RESULT_SUCCESS = 0;
        static final int RESULT_FAILED = 1;
        static final int RESULT_OFFLINE = 2;
        static final int RESULT_ERROR = 3;

        @Override
        protected Integer doInBackground(String... strings) {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                if (app.isOnLine()) {
                    final SyncTabRemoteService service = app.getSyncTabRemoteService();
                    final boolean result = service.register(strings[0], strings[1]);

                    return result ? RESULT_SUCCESS : RESULT_FAILED;
                }
                return RESULT_OFFLINE;
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to register");
                return RESULT_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);

            if (status == RESULT_SUCCESS) {
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                finish();
            }
            else if (status == RESULT_FAILED) {
                Toast.makeText(RegistrationActivity.this, R.string.failed_authenticate, 5000).show();
            }
            else if (status == RESULT_ERROR) {
                Toast.makeText(RegistrationActivity.this, R.string.error_authenticate, 5000).show();
            }
            else if (status == RESULT_OFFLINE) {
                Toast.makeText(RegistrationActivity.this, R.string.no_connection, 5000).show();
            }
        }
    }

}