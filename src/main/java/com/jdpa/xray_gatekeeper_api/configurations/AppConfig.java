package com.jdpa.xray_gatekeeper_api.configurations;

public class AppConfig {
    public static final int DEFAULT_RATE_LIMIT =  System.getenv("RATE_LIMIT") != null  ?  Integer.parseInt(System.getenv("RATE_LIMIT")) : 6;
    public static final long DEFAULT_RATE_DURATION = System.getenv("RATE_DURATION") != null  ?  Integer.parseInt(System.getenv("RATE_DURATION")) : 60000;
}
