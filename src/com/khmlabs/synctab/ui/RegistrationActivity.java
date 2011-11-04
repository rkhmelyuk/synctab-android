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
import com.khmlabs.synctab.RegistrationStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.SyncTabRemoteService;

public class RegistrationActivity extends GuestActivity {

    private static final String TAG = "RegistrationActivity";

    EditText emailInput;
    EditText passwordInput;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailInput = (EditText) findViewById(R.id.email);

        passwordInput = (EditText) findViewById(R.id.password);
        passwordInput.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    register();
                    return true;
                }
                return false;
            }
        });

        final Button registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                register();
            }
        });
    }

    private void register() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.length() != 0 && password.length() != 0) {
            new RegisterTask().execute(email, password);
        }
        else {
            Toast.makeText(this, R.string.email_password_required, 3000).show();
        }
    }

    private class RegisterTask extends AsyncTask<String, String, RegistrationStatus> {

        private ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final String message = getResources().getString(R.string.account_registration);
            progress = ProgressDialog.show(RegistrationActivity.this, null, message, true, false);
        }

        @Override
        protected RegistrationStatus doInBackground(String... strings) {
            try {
                final SyncTabApplication app = (SyncTabApplication) getApplication();
                final SyncTabRemoteService service = app.getSyncTabRemoteService();
                return service.register(strings[0], strings[1]);
            }
            catch (Exception e) {
                Log.e(TAG, "Failed to register");
                return null;
            }
        }

        @Override
        protected void onPostExecute(RegistrationStatus status) {
            super.onPostExecute(status);

            progress.dismiss();

            if (status == null) {
                Toast.makeText(RegistrationActivity.this, R.string.error_register, 5000).show();
            }
            else if (status.getStatus() == RegistrationStatus.Status.Succeed) {
                authenticate(status);
            }
            else if (status.getStatus() == RegistrationStatus.Status.Failed) {
                String failMessage = getResources().getString(R.string.failed_register);
                StringBuilder message = new StringBuilder(failMessage);
                if (status.getMessage() != null) {
                    message.append(": ").append(status.getMessage());
                }
                Toast.makeText(RegistrationActivity.this, message.toString(), 5000).show();
            }
            else if (status.getStatus() == RegistrationStatus.Status.Offline) {
                Toast.makeText(RegistrationActivity.this, R.string.no_connection, 5000).show();
            }
        }

        private void authenticate(RegistrationStatus status) {
            SyncTabApplication app = (SyncTabApplication) getApplication();
            SyncTabRemoteService service = app.getSyncTabRemoteService();

            if (service.authenticate(status.getEmail(), status.getPassword())) {
                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                finish();
            }
            else {
                Intent registerIntent = new Intent(RegistrationActivity.this, LoginActivity.class);
                registerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                registerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                startActivity(registerIntent);
                finish();
            }

        }
    }

}