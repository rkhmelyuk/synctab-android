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
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// TODO                            CLEANUP

public class SyncTabRemoteService {

    private static final String CONTEXT = "SyncTabRemoteService";

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

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("link", link));
            nameValuePairs.add(new BasicNameValuePair("device", AppConstants.SYNCTAB_DEVICE));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(host, post);
            boolean status = readResultStatus(response);

            if (!status) {
                Log.e(CONTEXT, "Failed to share a tab.");
            }
            return status;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(CONTEXT, "Error to share a tab.");
            return false;
        }
    }

    public boolean refreshSharedTabs() {
        try {
            final HttpClient client = new DefaultHttpClient();

            final long since = 0;
            final HttpGet get = new HttpGet("/synctab-server/api/getShareTabs?since=" + since);

            HttpResponse response = client.execute(host, get);
            boolean status = readResultStatus(response);

            if (!status) {
                Log.e(CONTEXT, "Failed to share a tab.");
                return false;
            }

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(CONTEXT, "Error to share a tab.");
            return false;
        }
    }

    private boolean readResultStatus(HttpResponse response) throws Exception {
        if (response.getStatusLine().getStatusCode() != 200) {
            return false;
        }
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 50);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        String status = object.getString("status");

        return status.equals("success");
    }
}
