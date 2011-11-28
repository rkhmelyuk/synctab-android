package com.khmlabs.synctab.tag;

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
    private static final String API_ADD_TAG = "/api/addTag";
    private static final String API_REMOVE_TAG = "/api/removeTag";
    private static final String API_RENAME_TAG = "/api/renameTag";

    protected static final String TAG_ID = "id";
    protected static final String NAME = "name";

    public RemoteTagManager(SyncTabApplication application, HttpHost host) {
        super(application, host);
    }

    /**
     * Add the tag with specified name.
     *
     * @param name the tag name.
     * @return tag id if was added, otherwise null.
     */
    public String addTag(String name) {
        if (!application.isOnLine()) {
            return null;
        }

        try {
            final HttpPost post = new HttpPost(API_ADD_TAG);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair(NAME, name));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to add a tag.");
            }

            JsonResponse json = readResponse(response);
            if (json.isSuccess()) {
                return json.getString("id");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error to add a tag", e);
        }

        return null;
    }

    /**
     * Renames the tag with specified id.
     *
     * @param tagId the id of the tag to rename.
     * @param newName the new tag name.
     * @return true if tag was added, otherwise false.
     */
    public boolean renameTag(String tagId, String newName) {
        if (!application.isOnLine()) {
            return false;
        }

        try {
            final HttpPost post = new HttpPost(API_RENAME_TAG);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair(TAG_ID, tagId));
            nameValuePairs.add(new BasicNameValuePair(NAME, newName));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to rename a tag.");
            }

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to rename a tag", e);
        }

        return false;
    }

    /**
     * Removes the tag with specified id.
     *
     * @param tagId the id of the tag to remove.
     * @return true if tag was removed, otherwise false.
     */
    public boolean removeTag(String tagId) {
        if (!application.isOnLine()) {
            return false;
        }

        try {
            final HttpPost post = new HttpPost(API_REMOVE_TAG);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair(TAG_ID, tagId));
            nameValuePairs.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            final HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(host, post);
            if (!successResponseStatus(response)) {
                Log.e(TAG, "Failed to remove a tag.");
            }

            return readResponse(response).isSuccess();
        }
        catch (Exception e) {
            Log.e(TAG, "Error to remove a tag", e);
        }

        return false;
    }

    public List<Tag> getTags() {
        if (!application.isOnLine()) {
            return Collections.emptyList();
        }

        try {
            final List<NameValuePair> params = new ArrayList<NameValuePair>(3);
            params.add(new BasicNameValuePair(TOKEN, application.getAuthToken()));

            String urlParams = "?" + URLEncodedUtils.format(params, "utf-8");
            final HttpGet get = new HttpGet(API_GET_TAGS + urlParams);

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
        JSONArray array = jsonResponse.getJson().getJSONArray("tags");
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
