package com.example.URLShortener.analytic;

public final class AnalyticsStreamConstants {

    public static final String STREAM_KEY = "urlshortener:stream:click_events";
    public static final String GROUP = "click-analytics";
    public static final String CONSUMER = "consumer-1";

    private AnalyticsStreamConstants() {
        throw new UnsupportedOperationException("Constants class");
    }
}
