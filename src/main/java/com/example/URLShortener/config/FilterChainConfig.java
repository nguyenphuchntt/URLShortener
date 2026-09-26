package com.example.URLShortener.config;

import com.example.URLShortener.controller.RedirectFilter;
import com.example.URLShortener.ratelimit.RateLimitFilter;
import com.example.URLShortener.ratelimit.RateLimitProperties;
import com.example.URLShortener.ratelimit.RateLimitService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class FilterChainConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimitService rateLimitService,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitService, properties, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(-200);
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RedirectFilter> redirectFilterRegistration(RedirectFilter filter) {
        FilterRegistrationBean<RedirectFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(-150);
        bean.addUrlPatterns("/*");
        return bean;
    }
    // security chain (-100)
}
