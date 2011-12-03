package com.khmlabs.synctab;

public interface AppConstants {

    //String SERVICE_URL = "http://192.168.1.101:8080";
    //String SERVICE_URL = "http://192.168.0.197:8080";
    String SERVICE_URL = "http://synctabapp.khmelyuk.com";

    boolean LOG = false;

    String LAST_SYNC_TIME = "LAST_SYNC_TIME";
    String LAST_SHARED_TAB_ID = "LAST_SHARED_TAB_ID";

    String LAST_RECEIVED_TIME = "LAST_RECEIVED_TIME";
    String LAST_RECEIVED_TAB_ID = "LAST_RECEIVED_TAB_ID";

    String LAST_CACHE_CLEANUP_TIME = "LAST_CACHE_CLEANUP_TIME";

    String OLDEST_SHARED_TAB_ID = "OLDEST_SHARED_TAB_ID";

    String AUTH_TOKEN = "AUTH_TOKEN";
    String AUTH_USER = "AUTH_USER";

    /** The property to check whether tags were loaded already. */
    String TAGS_LOADED = "TAGS_LOADED";

    /** The tag of this device. */
    String CURRENT_TAG = "CURRENT_TAG";

    /** The default tag for android device */
    String ANDROID_TAG_NAME = "Android";

    /** The device name for this app.*/
    String DEVICE_NAME = "Android";

    /** The key for refresh period preference. */
    String REFRESH_PERIOD = "REFRESH_PERIOD";

    /** The key for preference used to show/hide sharing status. */
    String SHOW_SHARING_STATUS = "SHOW_SHARING_STATUS";

    // 30 days
    long CACHE_CLEANUP_PERIOD = 1000L * 3600L * 24L * 30L;
}
