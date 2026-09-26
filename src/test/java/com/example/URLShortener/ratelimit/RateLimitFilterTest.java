package com.example.URLShortener.ratelimit;

import com.example.URLShortener.dto.response.ErrorResponse;
import com.example.URLShortener.entity.enums.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private static final String SERIALIZED = "{\"status\":429,\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}";

    @Mock RateLimitService rateLimitService;
    @Mock ObjectMapper objectMapper;

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties(); // enabled = true by default
        filter = new RateLimitFilter(rateLimitService, properties, objectMapper);
    }

    @Test
    void whenRejected_shouldReturn429InTheSharedErrorResponseShape() throws Exception {
        when(rateLimitService.tryConsume(anyString(), any())).thenReturn(RateLimitResult.rejected(30));
        when(objectMapper.writeValueAsString(any())).thenReturn(SERIALIZED);

        MockHttpServletResponse response = doFilter("POST", "/api/v1/auth/login");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo(SERIALIZED);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        ErrorResponse body = (ErrorResponse) captor.getValue();
        assertThat(body.getStatus()).isEqualTo(429);
        assertThat(body.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
        assertThat(body.getErrorMessage()).isEqualTo("Rate limit exceeded");
        assertThat(body.getPath()).isEqualTo("/api/v1/auth/login");
        assertThat(body.getTime()).isNotNull();
    }

    @Test
    void whenSerializationFails_shouldStillReturn429WithFallbackBody() throws Exception {
        when(rateLimitService.tryConsume(anyString(), any())).thenReturn(RateLimitResult.rejected(12));
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("cannot serialize"));

        MockHttpServletResponse response = doFilter("POST", "/api/v1/auth/login");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("12");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void whenAllowed_shouldSetRemainingHeaderAndContinueTheChain() throws Exception {
        when(rateLimitService.tryConsume(anyString(), any())).thenReturn(RateLimitResult.allowed(42));

        MockHttpServletResponse response = doFilter("GET", "/api/v1/urls/me");

        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("42");
        assertThat(response.getStatus()).isEqualTo(200);
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void whenDisabled_shouldBypassTheLimiterEntirely() throws Exception {
        properties.setEnabled(false);

        MockHttpServletResponse response = doFilter("POST", "/api/v1/auth/login");

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(rateLimitService);
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void shouldKeyTheBucketByClientIpAndEndpointType() throws Exception {
        when(rateLimitService.tryConsume(anyString(), any())).thenReturn(RateLimitResult.allowed(1));

        doFilter("POST", "/api/v1/auth/login");

        verify(rateLimitService).tryConsume("ip:127.0.0.1", "login");
    }

    private MockHttpServletResponse doFilter(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
