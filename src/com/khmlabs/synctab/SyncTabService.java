package com.khmlabs.synctab;

import android.util.Log;
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

import com.khmlabs.synctab.db.SyncTabDatabase;
import com.khmlabs.synctab.queue.QueueTask;
import com.khmlabs.synctab.queue.TaskType;
import com.khmlabs.synctab.tab.SharedTab;
import com.khmlabs.synctab.tab.SharedTabUtil;
import com.khmlabs.synctab.util.IOUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SyncTabService {

    private static final String TAG = "SyncTabService";

    private static final String API_AUTHORIZE = "/api/authorize";
    private static final String API_REGISTER = "/api/register";
    private static final String API_LOGOUT = "/api/logout";
    private static final String API_SHARE_TAB = "/api/shareTab";
    private static final String API_REMOVE_TAB = "/api/removeTab";
    private static final String API_RESHARE_TAB = "/api/reshareTab";
    private static final String API_GET_TABS_AFTER = "/api/getTabsAfter";
    private static final String API_GET_TABS_BEFORE = "/api/getTabsBefore";
    private static final String API_GET_LAST_TABS = "/api/getLastTabs";

    private static final String ID = "id";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";
    private static final String DEVICE = "device";

    private static final int PAGE_SIZE = 15;

    private final HttpHost host;
    private final SyncTabApplication application;

    public SyncTabService(SyncTabApplication app, String scheme, String hostname, int port) {
        this.application = app;
        this.host = new HttpHost(hostname, port, scheme);
    }

    public boolean authenticate(String email, String password) {
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

    public RegistrationStatus register(String email, String password) {
        final RegistrationStatus result = new RegistrationStatus(email, password);

        if (!application.isOnLine()) {
            result.setStatus(RegistrationStatus.Status.Offline);
        }
        else {
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

            return application.getTaskQueueManager().addLogoutTask(token);
        }
        return RemoteOpStatus.Success;
    }

    private boolean logoutOnServer(String token) {
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

            return readResponse(response).success;
        }
        catch (Exception e) {
            Log.e(TAG, "Error to logout.", e);
            return false;
        }
    }

    public RemoteOpStatus enqueueSync(String link) {
        if (application.isOnLine() && shareTab(link)) {
            return RemoteOpStatus.Success;
        }

        return application.getTaskQueueManager().addShareTabTask(link);
    }

    public boolean shareTab(String link) {
        try {
            final HttpPost post = new HttpPost(API_SHARE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair(DEVICE, AppConstants.ANDROID_SYNCTAB_DEVICE));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
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
            final long lastSyncTime = application.getLastSyncTime();

            final boolean firstRequest = (lastSharedTabId == null && lastSyncTime == 0);
            final String operation = firstRequest ? API_GET_LAST_TABS : API_GET_TABS_AFTER;
            final String paramString = "?" + buildGetSharedTabsParamsString(lastSharedTabId, lastSyncTime);

            final long syncTime = System.currentTimeMillis();
            final HttpGet get = new HttpGet(operation + paramString);

            if (handleRecentSharedTabs(get)) {
                application.setLastSyncTime(syncTime);
                return true;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh a shared tabs.", e);
        }
        return false;
    }

    private boolean handleRecentSharedTabs(HttpGet get) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(host, get);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to refresh a shared tabs.");
            return false;
        }

        JsonResponse jsonResponse = readResponse(response);
        if (jsonResponse.success) {
            List<SharedTab> sharedTabs = readSharedTabs(jsonResponse);
            if (sharedTabs.size() > 0) {
                insertSharedTabs(sharedTabs);
                updateRecentSharedTabId(sharedTabs);
                updateOldestSharedTabId(sharedTabs);
                cacheFavicons(sharedTabs);
            }
        }

        return jsonResponse.success;
    }

    public boolean getOlderSharedTabs() {
        if (!application.isOnLine()) {
            return true;
        }

        try {
            final String oldestTabId = application.getOldestSharedTabId();
            if (oldestTabId == null) {
                return false;
            }

            final String paramString = "?" + buildGetOlderSharedTabsParamsString(oldestTabId, PAGE_SIZE);
            final HttpGet get = new HttpGet(API_GET_TABS_BEFORE + paramString);

            return handleOlderSharedTabs(get);
        }
        catch (Exception e) {
            Log.e(TAG, "Error to load older shared tabs.", e);
            return false;
        }
    }

    private boolean handleOlderSharedTabs(HttpGet get) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(host, get);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to load older shared tabs.");
            return false;
        }

        final JsonResponse jsonResponse = readResponse(response);
        if (jsonResponse.success) {
            List<SharedTab> sharedTabs = readSharedTabs(jsonResponse);
            if (sharedTabs.size() > 0) {
                insertSharedTabs(sharedTabs);
                updateOldestSharedTabId(sharedTabs);
                cacheFavicons(sharedTabs);
            }
            else {
                application.setOldestSharedTabId(null);
            }
        }

        return true;
    }

    private String buildGetSharedTabsParamsString(String lastSharedTabId, long lastSyncTime) {
        final List<NameValuePair> params = new LinkedList<NameValuePair>();
        if (lastSharedTabId != null) {
            params.add(new BasicNameValuePair(ID, lastSharedTabId));
        }
        if (lastSyncTime != 0) {
            params.add(new BasicNameValuePair("ts", Long.toString(lastSyncTime)));
        }
        params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));

        return URLEncodedUtils.format(params, "utf-8");
    }

    private String buildGetOlderSharedTabsParamsString(String oldestSharedTabId, int max) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(new BasicNameValuePair(ID, oldestSharedTabId));
        params.add(new BasicNameValuePair("max", Integer.toString(max)));
        params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));

        return URLEncodedUtils.format(params, "utf-8");
    }

    private void cacheFavicons(List<SharedTab> tabs) {
        new FaviconPreloader(application).preloadForTabs(tabs);
    }

    private void updateRecentSharedTabId(List<SharedTab> sharedTabs) {
        final SharedTab recent = SharedTabUtil.getRecentSharedTab(sharedTabs);
        if (recent != null) {
            application.setLastSharedTabId(recent.getId());
        }
        else {
            application.setLastSharedTabId(null);
        }
    }

    private void updateOldestSharedTabId(List<SharedTab> sharedTabs) {
        final SharedTab oldest = SharedTabUtil.getOldestSharedTab(sharedTabs);
        if (oldest != null) {
            application.setOldestSharedTabId(oldest.getId());
        }
        else {
            application.setOldestSharedTabId(null);
        }
    }

    private void insertSharedTabs(List<SharedTab> sharedTabs) {
        if (sharedTabs.size() > 0) {
            SyncTabDatabase database = null;
            try {
                database = new SyncTabDatabase(application);
                database.replaceSharedTabs(sharedTabs);
            }
            finally {
                if (database != null) {
                    database.close();
                }
            }
        }
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
            else if (task.getType() == TaskType.LoadFavicon) {
                FaviconPreloader loader = new FaviconPreloader(application);
                return loader.preloadFavicon(task.getParam());
            }
        }

        return false;
    }

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

    public RemoteOpStatus removeSharedTab(int tabId) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            SharedTab sharedTab = database.getSharedTabById(tabId);
            if (sharedTab != null) {
                database.removeSharedTab(tabId);

                if (application.isOnLine()) {
                    if (removeSharedTabOnServer(sharedTab.getId())) {
                        return RemoteOpStatus.Success;
                    }
                }

                return application.getTaskQueueManager().addRemoveTabTask(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove shared tab.");
            return RemoteOpStatus.Failed;
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Success;
    }

    private boolean removeSharedTabOnServer(String tabId) {
        try {
            final HttpPost post = new HttpPost(API_REMOVE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(ID, tabId));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
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

    public RemoteOpStatus reshareTab(int tabId) {
        SyncTabDatabase database = null;
        try {
            database = new SyncTabDatabase(application);
            SharedTab sharedTab = database.getSharedTabById(tabId);
            if (sharedTab != null) {
                sharedTab.setTimestamp(System.currentTimeMillis());
                database.replaceSharedTab(sharedTab);

                if (application.isOnLine()) {
                    if (reshareTabOnServer(sharedTab.getId())) {
                        return RemoteOpStatus.Success;
                    }
                }

                return application.getTaskQueueManager().addReshareTabTask(sharedTab.getId());
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to reshare tab.");
            return RemoteOpStatus.Failed;
        }
        finally {
            if (database != null) {
                database.close();
            }
        }

        return RemoteOpStatus.Failed;
    }

    private boolean reshareTabOnServer(String tabId) {
        try {
            final HttpPost post = new HttpPost(API_RESHARE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(ID, tabId));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
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

    private static JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        if (AppConstants.LOG) Log.i(TAG, content);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        boolean success = object.getBoolean("status");

        return new JsonResponse(success, object);
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
