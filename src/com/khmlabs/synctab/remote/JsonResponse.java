package com.khmlabs.synctab.remote;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonResponse {
    final boolean success;
    final JSONObject json;

    public JsonResponse(boolean success, JSONObject json) {
        this.success = success;
        this.json = json;
    }

    public boolean isSuccess() {
        return success;
    }

    public JSONObject getJson() {
        return json;
    }

    public String getString(String name) throws JSONException {
        if (json.isNull(name)) {
            return null;
        }
        return json.getString(name);
    }
}