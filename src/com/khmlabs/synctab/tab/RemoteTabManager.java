package com.khmlabs.synctab.tab;

import android.util.Log;
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

import com.khmlabs.synctab.AppConstants;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.remote.JsonResponse;
import com.khmlabs.synctab.remote.RemoteManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ruslan Khmelyuk
 */
public class RemoteTabManager extends RemoteManager {

    private static final String TAG = "RemoteTabManager";

    private static final String API_SHARE_TAB = "/api/shareTab";
    private static final String API_REMOVE_TAB = "/api/removeTab";
    private static final String API_RESHARE_TAB = "/api/reshareTab";
    private static final String API_GET_TABS_AFTER = "/api/getTabsAfter";
    private static final String API_GET_TABS_BEFORE = "/api/getTabsBefore";
    private static final String API_GET_LAST_TABS = "/api/getLastTabs";

    public RemoteTabManager(SyncTabApplication application, HttpHost host) {
        super(application, host);
    }

    public boolean shareTab(String link, String tagId) {
        try {
            final HttpPost post = new HttpPost(API_SHARE_TAB);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair(TAG_ID, tagId));
            nameValuePairs.add(new BasicNameValuePair(DEVICE, AppConstants.DEVICE_NAME));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to share a tab.");
            }

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to share a tab.", e);
            return false;
        }
    }

    public boolean removeSharedTab(String tabId) {
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

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove tab.", e);
            return false;
        }
    }

    public boolean reshareTab(String tabId) {
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

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to reshare tab.", e);
            return false;
        }
    }

    public List<SharedTab> getSharedTabs() {
        if (!application.isOnLine()) {
            return null;
        }

        try {
            final String lastSharedTabId = application.getLastSharedTabId();
            final long lastSyncTime = application.getLastSyncTime();

            final boolean firstRequest = (lastSharedTabId == null && lastSyncTime == 0);
            final String operation = firstRequest ? API_GET_LAST_TABS : API_GET_TABS_AFTER;
            final String paramString = "?" + buildGetSharedTabsParamsString(
                    null, lastSharedTabId, lastSyncTime);

            final long syncTime = System.currentTimeMillis();
            final HttpGet get = new HttpGet(operation + paramString);

            List<SharedTab> tabs = getRecentSharedTabs(get);
            if (tabs.size() > 0) {
                application.setLastSyncTime(syncTime);

                final SharedTab recent = SharedTabUtil.getRecentSharedTab(tabs);
                final String tabId = recent != null ? recent.getId() : null;
                application.setLastSharedTabId(tabId);

                return tabs;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh a shared tabs.", e);
        }
        return null;
    }

    public List<SharedTab> receiveSharedTabs(String tagId) {
        if (!application.isOnLine()) {
            return null;
        }

        try {
            final String lastReceivedTabId = application.getLastReceivedTabId();
            final long lastReceivedTime = application.getLastReceivedTime();

            final boolean firstRequest = (lastReceivedTabId == null && lastReceivedTime == 0);
            final String operation = firstRequest ? API_GET_LAST_TABS : API_GET_TABS_AFTER;
            final String paramString = "?" + buildGetSharedTabsParamsString(
                    tagId, lastReceivedTabId, lastReceivedTime);

            final long syncTime = System.currentTimeMillis();
            final HttpGet get = new HttpGet(operation + paramString);

            List<SharedTab> tabs = getRecentSharedTabs(get);
            if (tabs != null && tabs.size() > 0) {
                application.setLastReceivedTime(syncTime);

                final SharedTab recent = SharedTabUtil.getRecentSharedTab(tabs);
                final String tabId = recent != null ? recent.getId() : null;
                application.setLastReceivedTabId(tabId);

                return tabs;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh a shared tabs.", e);
        }
        return null;
    }

    private List<SharedTab> getRecentSharedTabs(HttpGet get) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(host, get);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to refresh a shared tabs.");
            return null;
        }

        JsonResponse jsonResponse = readResponse(response);
        boolean success = jsonResponse.isSuccess();
        if (success) {
            return readSharedTabs(jsonResponse);
        }

        return null;
    }

    public List<SharedTab> getOlderSharedTabs() {
        if (!application.isOnLine()) {
            return null;
        }

        try {
            final String oldestTabId = application.getOldestSharedTabId();
            if (oldestTabId == null) {
                return null;
            }

            final String paramString = "?" + buildGetOlderSharedTabsParamsString(oldestTabId, PAGE_SIZE);
            final HttpGet get = new HttpGet(API_GET_TABS_BEFORE + paramString);

            return getOlderSharedTabs(get);
        }
        catch (Exception e) {
            Log.e(TAG, "Error to load older shared tabs.", e);
            return null;
        }
    }

    private List<SharedTab> getOlderSharedTabs(HttpGet get) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(host, get);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to load older shared tabs.");
            return null;
        }

        final JsonResponse jsonResponse = readResponse(response);
        if (jsonResponse.isSuccess()) {
            return readSharedTabs(jsonResponse);
        }

        return null;
    }

    private String buildGetSharedTabsParamsString(String tagId, String lastSharedTabId, long lastSyncTime) {
        final List<NameValuePair> params = new LinkedList<NameValuePair>();
        if (lastSharedTabId != null) {
            params.add(new BasicNameValuePair(ID, lastSharedTabId));
        }
        if (lastSyncTime != 0) {
            params.add(new BasicNameValuePair("ts", Long.toString(lastSyncTime)));
        }
        params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
        params.add(new BasicNameValuePair(TAG_ID, tagId));

        return URLEncodedUtils.format(params, "utf-8");
    }

    private String buildGetOlderSharedTabsParamsString(String oldestSharedTabId, int max) {
        final List<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(new BasicNameValuePair(ID, oldestSharedTabId));
        params.add(new BasicNameValuePair("max", Integer.toString(max)));
        params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));

        return URLEncodedUtils.format(params, "utf-8");
    }

    private static List<SharedTab> readSharedTabs(JsonResponse jsonResponse) throws Exception {
        JSONArray array = jsonResponse.getJson().getJSONArray("tabs");
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
        if (!row.isNull("tag")) {
            result.setTagId(row.getString("tag"));
        }
        if (!row.isNull("device")) {
            result.setDevice(row.getString("device"));
        }
        if (!row.isNull("title")) {
            result.setTitle(row.getString("title"));
        }

        return result;
    }
}
