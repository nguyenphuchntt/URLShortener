package com.example.URLShortener.analytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventPayload {
    private Long shortUrlId;
    private String ip;
    private String userAgent;
    private String referrer;
    private Long timestamp; // millis since epoch (clickedAt)
}
