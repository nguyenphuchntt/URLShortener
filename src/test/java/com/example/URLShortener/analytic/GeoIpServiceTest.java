package com.example.URLShortener.analytic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nạp GeoLite2-City.mmdb thật từ classpath ({@code src/main/resources/geoip/}).
 * Nếu file bị thiếu thì reader = null và mọi lookup trả UNKNOWN — khi đó
 * {@link #resolve_whenIpIsPublic_shouldResolveCountry()} sẽ fail để báo hiệu geo đang tắt.
 */
class GeoIpServiceTest {

    private static GeoIpService service;

    @BeforeAll
    static void loadDatabase() {
        service = new GeoIpService();
    }

    @AfterAll
    static void closeDatabase() {
        // close() nuốt mọi exception nên gọi hai lần phải không ném gì.
        service.close();
        service.close();
    }

    @Test
    void resolve_whenIpIsNull_shouldReturnUnknown() {
        assertThat(service.resolve(null)).isEqualTo(GeoIpService.GeoLocation.UNKNOWN);
    }

    @Test
    void resolve_whenIpIsBlank_shouldReturnUnknown() {
        for (String ip : List.of("", " ", "\t")) {
            assertThat(service.resolve(ip)).as("ip='%s'", ip).isEqualTo(GeoIpService.GeoLocation.UNKNOWN);
        }
    }

    @Test
    void resolve_whenIpIsLoopback_shouldReturnUnknown() {
        for (String ip : List.of("127.0.0.1", "::1")) {
            assertThat(service.resolve(ip)).as("ip=%s", ip).isEqualTo(GeoIpService.GeoLocation.UNKNOWN);
        }
    }

    @Test
    void resolve_whenIpIsSiteLocal_shouldReturnUnknown() {
        for (String ip : List.of("10.0.0.5", "172.16.31.9", "192.168.1.10")) {
            assertThat(service.resolve(ip)).as("ip=%s", ip).isEqualTo(GeoIpService.GeoLocation.UNKNOWN);
        }
    }

    @Test
    void resolve_whenIpIsLinkLocal_shouldReturnUnknown() {
        for (String ip : List.of("169.254.10.10", "fe80::1")) {
            assertThat(service.resolve(ip)).as("ip=%s", ip).isEqualTo(GeoIpService.GeoLocation.UNKNOWN);
        }
    }

    @Test
    void resolve_whenIpIsPublic_shouldResolveCountry() {
        GeoIpService.GeoLocation location = service.resolve("8.8.8.8");

        assertThat(location.isKnown()).isTrue();
        assertThat(location.country()).hasSize(2);
    }

    @Test
    void geolocation_whenCountryMissing_shouldNotBeKnown() {
        assertThat(GeoIpService.GeoLocation.UNKNOWN.isKnown()).isFalse();
        assertThat(new GeoIpService.GeoLocation(null, "Hanoi").isKnown()).isFalse();
        assertThat(new GeoIpService.GeoLocation("  ", "Hanoi").isKnown()).isFalse();
    }

    @Test
    void geolocation_whenCountryPresent_shouldBeKnown() {
        assertThat(new GeoIpService.GeoLocation("VN", "Hanoi").isKnown()).isTrue();
    }
}
