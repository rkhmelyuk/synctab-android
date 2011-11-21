package com.khmlabs.synctab.remote;

import android.util.Log;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.khmlabs.synctab.AppConstants;
import com.khmlabs.synctab.SyncTabApplication;
import com.khmlabs.synctab.util.IOUtil;

import java.io.InputStream;

/**
 * The base remote manager.
 *
 * @author Ruslan Khmelyuk
 */
public abstract class RemoteManager {

    private static final String TAG = "RemoteManager";

    protected static final String ID = "id";
    protected static final String TOKEN = "token";
    protected static final String TAG_ID = "tagId";

    protected static final int PAGE_SIZE = 15;

    protected final HttpHost host;
    protected final SyncTabApplication application;

    protected RemoteManager(SyncTabApplication application, HttpHost host) {
        this.application = application;
        this.host = host;
    }

    protected boolean successResponseStatus(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            application.setAuthToken(null);
            application.setAuthEmail(null);
            return false;
        }

        return statusCode == HttpStatus.SC_OK;
    }

    protected JsonResponse readResponse(HttpResponse response) throws Exception {
        InputStream contentStream = response.getEntity().getContent();
        String content = IOUtil.toString(contentStream, 200);

        if (AppConstants.LOG) Log.i(TAG, content);

        JSONObject object = (JSONObject) new JSONTokener(content).nextValue();
        boolean success = object.getBoolean("status");

        return new JsonResponse(success, object);
    }
}
