CREATE TABLE clicks_hourly (
    short_url_id BIGINT NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
    hour TIMESTAMP NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (short_url_id, hour)
);

CREATE INDEX idx_clicks_hourly_clicked_at ON clicks_hourly (hour DESC);

CREATE TABLE link_stats (
    short_url_id BIGINT PRIMARY KEY REFERENCES short_urls(id) ON DELETE CASCADE,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    last_click_at TIMESTAMP
);

CREATE TABLE click_country_stats (
    short_url_id BIGINT NOT NULL REFERENCES short_urls(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    country VARCHAR(2) NOT NULL,
    clicks BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (short_url_id, stat_date, country)
);
