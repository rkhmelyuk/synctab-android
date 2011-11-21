package com.khmlabs.synctab.tag;

import android.util.Log;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.remote.JsonResponse;
import com.khmlabs.synctab.remote.RemoteManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Remote tag manager used to get the data from cloud.
 *
 * @author Ruslan Khmelyuk
 */
public class RemoteTagManager extends RemoteManager {

    private static final String TAG = "RemoteTagManager";

    private static final String API_GET_TAGS = "/api/getTags";

    public RemoteTagManager(SyncTabApplication application, HttpHost host) {
        super(application, host);
    }

    public List<Tag> getTags() {
        if (!application.isOnLine()) {
            return Collections.emptyList();
        }

        try {
            final String operation = API_GET_TAGS;
            final List<NameValuePair> params = new ArrayList<NameValuePair>(3);
            params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));

            String urlParams = "?" + URLEncodedUtils.format(params, "utf-8");
            final HttpGet get = new HttpGet(operation + urlParams);

            return readRemoteTags(get);
        }
        catch (Exception e) {
            Log.e(TAG, "Error to refresh tags", e);
        }
        return Collections.emptyList();
    }

    private List<Tag> readRemoteTags(HttpGet get) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpResponse response = client.execute(host, get);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to load older shared tabs.");
            return Collections.emptyList();
        }

        final JsonResponse jsonResponse = readResponse(response);
        if (jsonResponse.isSuccess()) {
            return readTags(jsonResponse);
        }

        return Collections.emptyList();
    }

    private List<Tag> readTags(JsonResponse jsonResponse) throws JSONException {
        JSONArray array = jsonResponse.getJson().getJSONArray("tabs");
        if (array.length() == 0) {
            return Collections.emptyList();
        }

        final List<Tag> result = new ArrayList<Tag>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.getJSONObject(i);
            Tag tag = readTagFromJson(row);
            if (tag != null) {
                result.add(tag);
            }
        }

        return result;
    }

    private Tag readTagFromJson(JSONObject row) throws JSONException {
        final Tag result = new Tag();

        result.setTagId(row.getString("id"));
        result.setName(row.getString("name"));

        return result;
    }
}
