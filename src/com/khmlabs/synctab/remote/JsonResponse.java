package com.khmlabs.synctab.remote;

import org.json.JSONException;
import org.json.JSONObject;

class JsonResponse {
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