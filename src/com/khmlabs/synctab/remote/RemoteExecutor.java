package com.khmlabs.synctab.remote;

import android.util.Log;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.khmlabs.synctab.util.IOUtil;

import java.io.InputStream;

public class RemoteExecutor {

    private static final String TAG = "RemoteExecutor";

    private final HttpHost host;

    public RemoteExecutor(HttpHost host) {
        this.host = host;
    }

    public JsonResponse execute(HttpRequest request) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(host, request);
        if (!successResponseStatus(response)) {
            Log.e(TAG, "Failed to execute request: " + request.getRequestLine());
        }

        return readResponse(response);
    }

    private static JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        boolean success = object.getBoolean("status");

        return new JsonResponse(success, object);
    }

    private boolean successResponseStatus(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            // TODO - logout
            application.setAuthToken(null);
            application.setAuthEmail(null);
            return false;
        }

        return statusCode == HttpStatus.SC_OK;
    }
}
