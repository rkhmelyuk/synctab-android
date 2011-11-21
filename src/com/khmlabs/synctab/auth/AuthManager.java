package com.khmlabs.synctab.auth;

import android.util.Log;

import com.khmlabs.synctab.RegistrationStatus;
import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.db.SyncTabDatabase;

/**
 * @author Ruslan Khmelyuk
 */
public class AuthManager {

    private static final String TAG = "AuthManager";

    private final SyncTabApplication application;
    private final RemoteAuthManager remote;

    public AuthManager(SyncTabApplication application, RemoteAuthManager remote) {
        this.remote = remote;
        this.application = application;
    }

    /**
     * Authenticate (login) user into the system.
     *
     * @param email the user email address.
     * @param password the user password.
     * @return true if user is authenticated, otherwise false.
     */
    public boolean authenticate(String email, String password) {
        if (!application.isOnLine()) {
            return false;
        }

        try {
            String token =  remote.authenticate(email, password);
            if (token != null && token.length() > 0) {
                application.setAuthEmail(email);
                application.setAuthToken(token);

                return true;
            }
            else {
                Log.e(TAG, "Failed to authenticate");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to authenticate.", e);
        }
        return false;
    }

    /**
     * Logout user auth token.
     *
     * @param token the current user auth token.
     * @return the remote operation status.
     */
    public RemoteOpStatus logout(String token) {
        if (token != null) {
            removeUserData();

            if (application.isOnLine()) {
                if (remote.logout(token)) {
                    return RemoteOpStatus.Success;
                }
            }

            return application.getTaskQueueManager().addLogoutTask(token);
        }
        return RemoteOpStatus.Success;
    }

    /**
     * Register new user by email and password.
     *
     * @param email the user email address.
     * @param password the user password.
     * @return the registration status.
     */
    public RegistrationStatus register(String email, String password) {
        if (!application.isOnLine()) {
            final RegistrationStatus result = new RegistrationStatus(email, password);
            result.setStatus(RegistrationStatus.Status.Offline);
            return result;
        }

        return remote.register(email, password);
    }

    /**
     * Remove current user data.
     */
    public void removeUserData() {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            database.removeUserData();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove user data", e);
        }
        finally {
            if (database != null) {
                database.close();
            }
        }
    }
}
