package com.khmlabs.synctab;

public interface AppConstants {

    String SERVICE_URL = "http://192.168.0.197:8080";
    //String SERVICE_URL = "http://192.168.1.102:8080";
    //String SERVICE_URL = "http://synctabapp.khmelyuk.com";

    String ANDROID_SYNCTAB_DEVICE = "synctab-android-app";

    String LAST_SYNC_TIME = "LAST_SYNC_TIME";

    String LAST_CACHE_CLEANUP_TIME = "LAST_CACHE_CLEANUP_TIME";

    String LAST_SHARED_TAB_ID = "LAST_SHARED_TAB_ID";
    String OLDEST_SHARED_TAB_ID = "OLDEST_SHARED_TAB_ID";

    String AUTH_TOKEN = "AUTH_TOKEN";
    String AUTH_USER = "AUTH_USER";

    // 30 days
    long CACHE_CLEANUP_PERIOD = 1000L * 3600L * 24L * 30L;

}
