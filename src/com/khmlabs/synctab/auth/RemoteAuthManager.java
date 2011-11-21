package com.khmlabs.synctab.auth;

import android.util.Log;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.khmlabs.synctab.RegistrationStatus;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.remote.JsonResponse;
import com.khmlabs.synctab.remote.RemoteManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ruslan Khmelyuk
 */
public class RemoteAuthManager extends RemoteManager {

    private static final String TAG = "RemoteAuthManager";

    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    private static final String API_AUTHORIZE = "/api/authorize";
    private static final String API_REGISTER = "/api/register";
    private static final String API_LOGOUT = "/api/logout";

    public RemoteAuthManager(SyncTabApplication application, HttpHost host) {
        super(application, host);
    }

    public String authenticate(String email, String password) {
        try {
            final HttpPost post = new HttpPost(API_AUTHORIZE);

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(EMAIL, email));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                JsonResponse jsonResponse = readResponse(response);
                if (jsonResponse.isSuccess()) {
                    return jsonResponse.getString(TOKEN);
                }
            }
            else {
                Log.e(TAG, "Failed to authenticate");
            }

        }
        catch (Exception e) {
            Log.e(TAG, "Error to authenticate.", e);
        }
        return null;
    }

    public RegistrationStatus register(String email, String password) {
        final RegistrationStatus result = new RegistrationStatus(email, password);
        result.setStatus(RegistrationStatus.Status.Failed);

        try {
            final HttpPost post = new HttpPost(API_REGISTER);

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(EMAIL, email));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                JsonResponse json = readResponse(response);
                RegistrationStatus.Status status = json.isSuccess()
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

        return result;
    }

    public boolean logout(String token) {
        try {
            final HttpPost post = new HttpPost(API_LOGOUT);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair(TOKEN, token));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to logout.");
            }

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to logout.", e);
            return false;
        }
    }
}
