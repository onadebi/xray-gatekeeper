package com.jdpa.xray_gatekeeper_api.xray.models;

import java.lang.reflect.Constructor;

public class XrayAuth {
    private String client_id;
    private String client_secret;

    public XrayAuth() {}

    public XrayAuth(String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }
    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    @Override
    public String toString() {
        return "XrayAuth{" +
                "client_id='" + client_id + '\'' +
                '}';
    }

}
