package com.example.URLShortener.analytic;

public final class AnalyticsStreamConstants {

    public static final String STREAM_KEY = "urlshortener:stream:click_events";
    /** Entries that could not be processed (malformed or retries exhausted). Inspected manually. */
    public static final String DLQ_STREAM_KEY = STREAM_KEY + ":dlq";
    public static final String GROUP = "click-analytics";
    /** Field of each stream entry holding the JSON-serialized {@link ClickEventPayload}. */
    public static final String PAYLOAD_FIELD = "payload";

    private AnalyticsStreamConstants() {
        throw new UnsupportedOperationException("Constants class");
    }
}
