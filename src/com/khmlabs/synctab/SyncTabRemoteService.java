package com.khmlabs.synctab;

import android.util.Log;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.util.IOUtil;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SyncTabRemoteService {

    private static final String TAG = "SyncTabRemoteService";

    private final HttpHost host;
    private final SyncTabApplication application;

    public SyncTabRemoteService(SyncTabApplication app, String scheme, String hostname, int port) {
        this.application = app;
        this.host = new HttpHost(hostname, port, scheme);
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
                if (jsonResponse.success) {
                    String token = jsonResponse.getString("token");
                    if (token != null && token.length() > 0 && !token.equals("null")) {
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

    public RemoteOpState logout(String token) {
        if (token != null) {
            if (application.isOnLine()) {
                if (logoutOnServer(token)) {
                    return RemoteOpState.Success;
                }
            }

            return addLogoutToQueue(token);
        }
        return RemoteOpState.Success;
    }

    private boolean logoutOnServer(String token) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpPost post = new HttpPost("/synctab-server/api/logout");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("token", token));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to logout.");
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error to logout.", e);
            return false;
        }
    }

    public RemoteOpState enqueueSync(String link) {
        if (application.isOnLine() && shareTab(link)) {
            return RemoteOpState.Success;
        }

        return addShareTabToQueue(link);
    }

    public boolean shareTab(String link) {
        try {
            final HttpClient client = new DefaultHttpClient();
            final HttpPost post = new HttpPost("/synctab-server/api/shareTab");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair("device", AppConstants.SYNCTAB_DEVICE));
            nameValuePairs.add(new BasicNameValuePair("token", application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to share a tab.");
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error to share a tab.", e);
            return false;
        }
    }

    public boolean refreshSharedTabs() {
        try {
            final HttpClient client = new DefaultHttpClient();

            final long since = application.getLastSyncTime();

            List<NameValuePair> params = new LinkedList<NameValuePair>();
            params.add(new BasicNameValuePair("since", Long.toString(since)));
            params.add(new BasicNameValuePair("token", application.getAuthToken()));
            String paramString = URLEncodedUtils.format(params, "utf-8");

            final long syncTime = System.currentTimeMillis();
            final HttpGet get = new HttpGet("/synctab-server/api/getSharedTabs?" + paramString);

            HttpResponse response = client.execute(host, get);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to refresh a shared tabs.");
                return false;
            }

            JsonResponse jsonResponse = readResponse(response);
            if (jsonResponse.success) {
                List<SharedTab> sharedTabs = readSharedTabs(jsonResponse);
                insertSharedTabs(sharedTabs);
                application.setLastSyncTime(syncTime);
            }

            return jsonResponse.success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh a shared tabs.", e);
            return false;
        }
    }

    private List<SharedTab> readSharedTabs(JsonResponse jsonResponse) throws Exception {
        JSONArray array = jsonResponse.json.getJSONArray("tabs");
        if (array.length() == 0) {
            return Collections.emptyList();
        }

        final List<SharedTab> result = new ArrayList<SharedTab>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.getJSONObject(i);
            SharedTab tab = readSharedTabFromJson(row);
            if (tab != null) {
                result.add(tab);
            }
        }

        return result;
    }

    private SharedTab readSharedTabFromJson(JSONObject row) throws JSONException {
        final SharedTab result = new SharedTab();

        result.setId(row.getString("id"));
        result.setTimestamp(row.getLong("ts"));
        result.setLink(row.getString("link"));
        result.setTitle(row.getString("title"));
        result.setTitle(row.getString("title"));

        return result;
    }

    private void insertSharedTabs(List<SharedTab> sharedTabs) {
        if (sharedTabs.size() > 0) {
            DbHelper dbHelper = null;
            try {
                dbHelper = new DbHelper(application);
                dbHelper.insertSharedTabs(sharedTabs);
            }
            finally {
                if (dbHelper != null) {
                    dbHelper.close();
                }
            }
        }
    }

    private RemoteOpState addShareTabToQueue(String link) {
        return addTask(new QueueTask(TaskType.SyncTab, link));
    }

    private RemoteOpState addLogoutToQueue(String token) {
        return addTask(new QueueTask(TaskType.Logout, token));
    }

    private RemoteOpState addTask(QueueTask task) {
        DbHelper dbHelper = null;
        try {
            dbHelper = new DbHelper(application);
            dbHelper.insertQueueTask(task);

            Log.i(TAG, "Added task to QUEUE: " + task.getType() + " -> " + task.getParam());

            return RemoteOpState.Queued;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add sync tab task to queue", e);
        }
        finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        return RemoteOpState.Failed;
    }

    private JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        Log.i(TAG, content);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        boolean success = object.getBoolean("status");
        return new JsonResponse(success, object);
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

    public boolean syncTask(QueueTask task) {
        if (task != null) {
            if (task.getType() == TaskType.SyncTab) {
                return shareTab(task.getParam());
            }
            else if (task.getType() == TaskType.Logout) {
                return logoutOnServer(task.getParam());
            }
        }

        return false;
    }

    public void removeUserData() {
        DbHelper dbHelper = null;
        try {
            dbHelper = new DbHelper(application);
            dbHelper.removeUserData();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove user data", e);
        }
        finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }
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
