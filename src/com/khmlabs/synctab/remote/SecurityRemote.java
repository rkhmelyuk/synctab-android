package com.khmlabs.synctab.remote;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.khmlabs.synctab.RegistrationStatus;
import com.khmlabs.synctab.RemoteOpStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.queue.TaskQueueManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The remote operation to work with security part.
 */
public class SecurityRemote {

    private static final String TAG = "SecurityRemote";

    private final SyncTabApplication application;
    private final TaskQueueManager taskQueueManager;

    public SecurityRemote(SyncTabApplication application) {
        this.application = application;
        this.taskQueueManager = application.getTaskQueueManager();
    }

    public boolean authenticate(String email, String password) {
        try {
            final HttpPost post = new HttpPost(ApiContract.Action.AUTHORIZE);

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(ApiContract.Param.EMAIL, email));
            nameValuePairs.add(new BasicNameValuePair(ApiContract.Param.PASSWORD, password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                JsonResponse jsonResponse = readResponse(response);
                if (jsonResponse.success) {
                    String token = jsonResponse.getString(ApiContract.Param.TOKEN);
                    if (token != null && token.length() > 0) {
                        application.setAuthEmail(email);
                        application.setAuthToken(token);

                        return true;
                    }
                }
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

    public RegistrationStatus register(String email, String password) {
        final RegistrationStatus result = new RegistrationStatus(email, password);

        if (!application.isOnLine()) {
            result.setStatus(RegistrationStatus.Status.Offline);
        }
        else {
            result.setStatus(RegistrationStatus.Status.Failed);

            try {
                final HttpPost post = new HttpPost(ApiContract.Action.REGISTER);

                final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair(ApiContract.Param.EMAIL, email));
                nameValuePairs.add(new BasicNameValuePair(ApiContract.Param.PASSWORD, password));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                final HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(host, post);
                if (successResponseStatus(response)) {
                    JsonResponse json = readResponse(response);
                    RegistrationStatus.Status status = json.success
                            ? RegistrationStatus.Status.Succeed
                            : RegistrationStatus.Status.Failed;

                    result.setStatus(status);
                    result.setMessage(json.getString("message"));
                }
                else {
                    Log.e(TAG, "Failed to register");
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Error to register.", e);
            }
        }

        return result;
    }

    public RemoteOpStatus logout(String token) {
        if (token != null) {
            if (application.isOnLine()) {
                if (logoutOnServer(token)) {
                    return RemoteOpStatus.Success;
                }
            }

            return taskQueueManager.addLogoutTask(token);
        }
        return RemoteOpStatus.Success;
    }

    private boolean logoutOnServer(String token) {
        try {
            final HttpPost post = new HttpPost(ApiContract.Action.LOGOUT);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair(ApiContract.Param.TOKEN, token));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to logout.");
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to logout.", e);
            return false;
        }
    }
}
