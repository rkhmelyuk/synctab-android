package com.khmlabs.synctab;

import android.util.Log;
import com.khmlabs.synctab.util.IOUtil;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// TODO                            CLEANUP

public class SyncTabRemoteService {

    private static final String TAG = "SyncTabRemoteService";

    private final HttpHost host;
    private final SyncTabApplication application;

    public SyncTabRemoteService(SyncTabApplication app, String scheme, String hostname, int port) {
        this.application = app;
        this.host = new HttpHost(hostname, port, scheme);
    }

    public boolean enqueueSync(String link) {
        System.out.println(application.isOnLine());
        if (application.isOnLine() && shareTab(link)) {
            return true;
        }

        // TODO - enqueue tab to sync later  - save in database

        return true;
    }

    public boolean shareTab(String link) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpPost post = new HttpPost("/synctab-server/api/shareTab");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair("device", AppConstants.SYNCTAB_DEVICE));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to share a tab.");
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error to share a tab.");
            return false;
        }
    }

    public boolean refreshSharedTabs() {
        try {
            final HttpClient client = new DefaultHttpClient();

            final long since = 0;
            final HttpGet get = new HttpGet("/synctab-server/api/getShareTabs?since=" + since);

            HttpResponse response = client.execute(host, get);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to share a tab.");
                return false;
            }

            JsonResponse jsonResponse = readResponse(response);
            // TODO - fill shared tabs

            return jsonResponse.success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to share a tab.");
            return false;
        }
    }

    public boolean authenticate(String email, String password) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpPost post = new HttpPost("/synctab-server/api/authorize");

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("email", email));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                JsonResponse jsonResponse = readResponse(response);
                String token = jsonResponse.getString("token");
                if (token != null && token.length() > 0) {
                    application.setAuthEmail(email);
                    application.setAuthToken(token);

                    return true;
                }
            }
            else {
                Log.e(TAG, "Error to authenticate");
            }

        }
        catch (Exception e) {
            Log.e(TAG, "Error to authorize.");
        }
        return false;
    }

    private boolean successResponseStatus(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 401) {
            application.setAuthToken(null);
            application.setAuthEmail(null);
            return false;
        }

        return statusCode == 200;
    }

    private JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        String status = object.getString("status");
        boolean success = "success".equals(status);

        return new JsonResponse(success, object);
    }

    private static class JsonResponse {
        final boolean success;
        final JSONObject json;

        JsonResponse(boolean success, JSONObject json) {
            this.success = success;
            this.json = json;
        }

        String getString(String name) throws JSONException {
            return json.getString(name);
        }
    }
}
