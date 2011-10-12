package com.khmlabs.synctab;

import android.util.Log;
import com.khmlabs.synctab.db.DbHelper;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.util.IOUtil;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SyncTabRemoteService {

    private static final String TAG = "SyncTabRemoteService";

    private static final String API_AUTHORIZE = "/api/authorize";
    private static final String API_REGISTER = "/api/register";
    private static final String API_LOGOUT = "/api/logout";
    private static final String API_SHARE_TAB = "/api/shareTab";
    private static final String API_REMOVE_TAB = "/api/removeTab";
    private static final String API_RESHARE_TAB = "/api/reshareTab";
    private static final String API_GET_SHARED_TABS_SINCE = "/api/getSharedTabsSince";
    private static final String API_GET_SHARED_TABS_AFTER = "/api/getSharedTabsAfter";

    private static final String ID = "id";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";
    private static final String DEVICE = "device";

    private final HttpHost host;
    private final HttpClient client;
    private final SyncTabApplication application;

    public SyncTabRemoteService(SyncTabApplication app, String scheme, String hostname, int port) {
        this.application = app;
        this.host = new HttpHost(hostname, port, scheme);
        this.client = new DefaultHttpClient();
    }

    public boolean authenticate(String email, String password) {
        try {
            final HttpPost post = new HttpPost(API_AUTHORIZE);

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(EMAIL, email));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                JsonResponse jsonResponse = readResponse(response);
                if (jsonResponse.success) {
                    String token = jsonResponse.getString(TOKEN);
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

    public boolean register(String email, String password) {
        try {
            final HttpPost post = new HttpPost(API_REGISTER);

            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(EMAIL, email));
            nameValuePairs.add(new BasicNameValuePair(PASSWORD, password));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (successResponseStatus(response)) {
                return readResponse(response).success;
            }
            else {
                Log.e(TAG, "Failed to register");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to register.", e);
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
            final HttpPost post = new HttpPost(API_LOGOUT);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair(TOKEN, token));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

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

    public RemoteOpState enqueueSync(String link) {
        if (application.isOnLine() && shareTab(link)) {
            return RemoteOpState.Success;
        }

        return addShareTabToQueue(link);
    }

    public boolean shareTab(String link) {
        try {
            final HttpPost post = new HttpPost(API_SHARE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair(DEVICE, AppConstants.ANDROID_SYNCTAB_DEVICE));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to share a tab.");
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to share a tab.", e);
            return false;
        }
    }

    public boolean refreshSharedTabs() {
        if (!application.isOnLine()) {
            return true;
        }

        try {
            final String lastSharedTabId = application.getLastSharedTabId();
            final String paramString = buildGetSharedTabsParamsString(lastSharedTabId);
            final String operation = lastSharedTabId != null ? API_GET_SHARED_TABS_AFTER : API_GET_SHARED_TABS_SINCE;

            final long syncTime = System.currentTimeMillis();
            final HttpGet get = new HttpGet(operation + "?" + paramString);

            HttpResponse response = client.execute(host, get);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to refresh a shared tabs.");
                return false;
            }

            JsonResponse jsonResponse = readResponse(response);
            if (jsonResponse.success) {
                List<SharedTab> sharedTabs = readSharedTabs(jsonResponse);
                if (sharedTabs.size() > 0) {
                    insertSharedTabs(sharedTabs);
                    updateLastSharedTabId(sharedTabs);
                    cacheFavicons(sharedTabs);
                }
                application.setLastSyncTime(syncTime);
            }

            return jsonResponse.success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh a shared tabs.", e);
            return false;
        }
    }

    private void cacheFavicons(List<SharedTab> tabs) {
        for (SharedTab each : tabs) {
            String favicon = each.getFavicon();
            if (favicon != null && favicon.length() > 0) {
                downloadAndCacheFavicon(favicon);
            }
        }
    }

    private void downloadAndCacheFavicon(String url) {
        try {
            final HttpResponse response = client.execute(new HttpGet(url));
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                application.getCacheManager().store(url, response.getEntity().getContent());
            }
        }
        catch (IOException e) {
            Log.w(TAG, "Error to download favicon " + url);
        }
    }

    private String buildGetSharedTabsParamsString(String lastSharedTabId) {
        final List<NameValuePair> params = new LinkedList<NameValuePair>();
        if (lastSharedTabId != null) {
            params.add(new BasicNameValuePair(ID, lastSharedTabId));
        }
        else {
            final long since = application.getLastSyncTime();
            params.add(new BasicNameValuePair("since", Long.toString(since)));
        }
        params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
        return URLEncodedUtils.format(params, "utf-8");
    }

    private void updateLastSharedTabId(List<SharedTab> sharedTabs) {
        final SharedTab recent = getRecentSharedTab(sharedTabs);
        if (recent != null) {
            application.setLastSharedTabId(recent.getId());
        }
        else {
            application.setLastSharedTabId(null);
        }
    }

    private void insertSharedTabs(List<SharedTab> sharedTabs) {
        if (sharedTabs.size() > 0) {
            DbHelper dbHelper = null;
            try {
                dbHelper = new DbHelper(application);
                dbHelper.replaceSharedTabs(sharedTabs);
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

    private boolean successResponseStatus(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            application.setAuthToken(null);
            application.setAuthEmail(null);
            return false;
        }

        return statusCode == HttpStatus.SC_OK;
    }

    public boolean syncTask(QueueTask task) {
        if (task != null) {
            if (task.getType() == TaskType.SyncTab) {
                return shareTab(task.getParam());
            }
            else if (task.getType() == TaskType.Logout) {
                return logoutOnServer(task.getParam());
            }
            else if (task.getType() == TaskType.RemoveSharedTab) {
                return removeSharedTabOnServer(task.getParam());
            }
            else if (task.getType() == TaskType.ReshareTab) {
                return reshareTabOnServer(task.getParam());
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

    public RemoteOpState removeSharedTab(int tabId) {
        DbHelper dbHelper = null;
        try {
            dbHelper = new DbHelper(application);
            SharedTab sharedTab = dbHelper.getSharedTabById(tabId);
            if (sharedTab != null) {
                dbHelper.removeSharedTab(tabId);

                if (application.isOnLine()) {
                    if (removeSharedTabOnServer(sharedTab.getId())) {
                        return RemoteOpState.Success;
                    }
                }

                return addRemoveTabToQueue(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove shared tab.");
            return RemoteOpState.Failed;
        }
        finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        return RemoteOpState.Success;
    }

    private boolean removeSharedTabOnServer(String tabId) {
        try {
            final HttpPost post = new HttpPost(API_REMOVE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(ID, tabId));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to remove tab.");
                return false;
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove tab.", e);
            return false;
        }
    }

    private RemoteOpState addRemoveTabToQueue(String sharedTabId) {
        return addTask(new QueueTask(TaskType.RemoveSharedTab, sharedTabId));
    }

    public RemoteOpState reshareTab(int tabId) {
        DbHelper dbHelper = null;
        try {
            dbHelper = new DbHelper(application);
            SharedTab sharedTab = dbHelper.getSharedTabById(tabId);
            if (sharedTab != null) {
                sharedTab.setTimestamp(System.currentTimeMillis());
                dbHelper.replaceSharedTab(sharedTab);

                if (application.isOnLine()) {
                    if (reshareTabOnServer(sharedTab.getId())) {
                        return RemoteOpState.Success;
                    }
                }

                return addReshareTabToQueue(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to reshare tab.");
            return RemoteOpState.Failed;
        }
        finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

        return RemoteOpState.Failed;
    }

    private boolean reshareTabOnServer(String tabId) {
        try {
            final HttpPost post = new HttpPost(API_RESHARE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(ID, tabId));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to reshare tab.");
                return false;
            }

            return readResponse(response).success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to reshare tab.", e);
            return false;
        }
    }

    private RemoteOpState addReshareTabToQueue(String sharedTabId) {
        return addTask(new QueueTask(TaskType.ReshareTab, sharedTabId));
    }

    private static JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        Log.i(TAG, content);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        boolean success = object.getBoolean("status");

        return new JsonResponse(success, object);
    }

    private static SharedTab getRecentSharedTab(List<SharedTab> sharedTabs) {
        long max = 0;
        SharedTab recent = null;

        for (SharedTab each : sharedTabs) {
            if (each.getTimestamp() > max) {
                max = each.getTimestamp();
                recent = each;
            }
        }

        return recent;
    }

    private static List<SharedTab> readSharedTabs(JsonResponse jsonResponse) throws Exception {
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

    private static SharedTab readSharedTabFromJson(JSONObject row) throws JSONException {
        final SharedTab result = new SharedTab();

        result.setId(row.getString("id"));
        result.setTimestamp(row.getLong("ts"));
        result.setLink(row.getString("link"));

        if (!row.isNull("favicon")) {
            result.setFavicon(row.getString("favicon"));
        }
        if (!row.isNull("device")) {
            result.setDevice(row.getString("device"));
        }
        if (!row.isNull("title")) {
            result.setTitle(row.getString("title"));
        }

        return result;
    }

    private static class JsonResponse {
        final boolean success;
        final JSONObject json;

        JsonResponse(boolean success, JSONObject json) {
            this.success = success;
            this.json = json;
        }

        String getString(String name) throws JSONException {
            if (json.isNull(name)) {
                return null;
            }
            return json.getString(name);
        }
    }
}
