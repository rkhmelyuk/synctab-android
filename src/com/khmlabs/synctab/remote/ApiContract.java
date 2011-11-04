package com.khmlabs.synctab.remote;

public interface ApiContract {

    public interface Action {
        String AUTHORIZE = "/api/authorize";
        String REGISTER = "/api/register";
        String LOGOUT = "/api/logout";

        String SHARE_TAB = "/api/shareTab";
        String REMOVE_TAB = "/api/removeTab";
        String RESHARE_TAB = "/api/reshareTab";

        String GET_TABS_AFTER = "/api/getTabsAfter";
        String GET_TABS_BEFORE = "/api/getTabsBefore";
        String GET_LAST_TABS = "/api/getLastTabs";
    }

    public interface Param {

        String ID = "id";
        String EMAIL = "email";
        String PASSWORD = "password";
        String TOKEN = "token";
        String DEVICE = "device";
    }

}
