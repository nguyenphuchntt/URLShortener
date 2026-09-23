package com.example.URLShortener.analytic;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

@Slf4j
@Service
public class GeoIpService {

    private static final String DB_PATH = "geoip/GeoLite2-City.mmdb";

    private final DatabaseReader reader;

    public GeoIpService() {
        DatabaseReader r = null;
        try {
            ClassPathResource resource = new ClassPathResource(DB_PATH);
            r = new DatabaseReader.Builder(resource.getInputStream()).build();
            log.info("GeoIP database loaded from classpath:{}", DB_PATH);
        } catch (Exception e) {
            log.warn("GeoIP database not available at classpath:{}, geo lookups disabled", DB_PATH, e);
        }
        this.reader = r;
    }

    public GeoLocation resolve(String ip) {
        if (reader == null || ip == null || ip.isBlank()) {
            return GeoLocation.UNKNOWN;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                return GeoLocation.UNKNOWN;
            }
            CityResponse response = reader.city(address);
            String country = response.getCountry() != null ? response.getCountry().getIsoCode() : null;
            String city = response.getCity() != null ? response.getCity().getName() : null;
            if (country == null || country.isBlank()) {
                return GeoLocation.UNKNOWN;
            }
            return new GeoLocation(country, city);
        } catch (Exception e) {
            log.debug("Failed to resolve geo for ip={}, skipping country stat", ip);
            return GeoLocation.UNKNOWN;
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close GeoIP database reader", e);
        }
    }

    public record GeoLocation(String country, String city) {
        public static final GeoLocation UNKNOWN = new GeoLocation(null, null);

        public boolean isKnown() {
            return country != null && !country.isBlank();
        }
    }
}
