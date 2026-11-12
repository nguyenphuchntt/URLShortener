ALTER TABLE click_event   ALTER COLUMN clicked_at    TYPE TIMESTAMPTZ USING clicked_at    AT TIME ZONE 'UTC';
ALTER TABLE clicks_hourly ALTER COLUMN hour          TYPE TIMESTAMPTZ USING hour          AT TIME ZONE 'UTC';
ALTER TABLE link_stats    ALTER COLUMN last_click_at TYPE TIMESTAMPTZ USING last_click_at AT TIME ZONE 'UTC';
