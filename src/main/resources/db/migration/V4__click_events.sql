CREATE TABLE click_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code VARCHAR(20) NOT NULL,
    clicked_at TIMESTAMP NOT NULL DEFAULT now(),

    ip_address VARCHAR(45),
    user_agent TEXT,
    referrer TEXT,

    country VARCHAR(2),
    city VARCHAR(100)
);

CREATE INDEX idx_click_event_short_code_clicked_at
    ON click_event (short_code, clicked_at DESC);

CREATE INDEX idx_click_event_clicked_at
    ON click_event (clicked_at DESC);
