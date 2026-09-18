package com.example.URLShortener;

import com.example.URLShortener.ratelimit.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class UrlShortenerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}

}
