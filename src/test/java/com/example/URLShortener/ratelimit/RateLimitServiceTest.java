package com.example.URLShortener.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code RemoteBucketBuilder#build} trả về {@code BucketProxy} (một subtype của {@code Bucket},
 * không phải chính {@code Bucket}), nên builder được mock ở chế độ deep-stub để Mockito tự tạo
 * đúng type cho bucket thay vì phải import class đó.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock ProxyManager<String> proxyManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) RemoteBucketBuilder<String> bucketBuilder;
    @Mock ConsumptionProbe probe;

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        // BucketConfigurationFactory thật: BucketConfiguration chỉ là value object, không cần mock.
        service = new RateLimitService(proxyManager, new BucketConfigurationFactory(), new RateLimitProperties());
        // lenient: test thuần value object của RateLimitResult không đi qua ProxyManager.
        lenient().when(proxyManager.builder()).thenReturn(bucketBuilder);
    }

    @Test
    void tryConsume_whenTokenAvailable_shouldAllowAndReportRemaining() {
        givenProbe(true, 99L, 0L);

        RateLimitResult result = service.tryConsume("1.2.3.4", "default");

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemainingTokens()).isEqualTo(99L);
        assertThat(result.getRetryAfterSeconds()).isZero();
    }

    @Test
    void tryConsume_shouldBuildBucketWithIdentifierAndEndpointTypeKey() {
        givenProbe(true, 1L, 0L);

        service.tryConsume("1.2.3.4", "login");

        verify(bucketBuilder).build(eq("1.2.3.4:login"), any(BucketConfiguration.class));
    }

    @Test
    void tryConsume_whenEndpointTypeIsNull_shouldFallBackToDefaultLimit() {
        givenProbe(true, 5L, 0L);

        RateLimitResult result = service.tryConsume("1.2.3.4", null);

        assertThat(result.isAllowed()).isTrue();
        verify(bucketBuilder).build(eq("1.2.3.4:null"), any(BucketConfiguration.class));
    }

    @Test
    void tryConsume_whenBucketExhausted_shouldRejectWithRoundedUpRetryAfter() {
        givenProbe(false, 0L, 2_500_000_000L); // chờ 2.5s

        RateLimitResult result = service.tryConsume("1.2.3.4", "default");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemainingTokens()).isZero();
        assertThat(result.getRetryAfterSeconds()).isEqualTo(3L);
    }

    @Test
    void tryConsume_whenWaitIsSubSecond_shouldRetryAfterAtLeastOneSecond() {
        givenProbe(false, 0L, 1L); // 1ns

        assertThat(service.tryConsume("1.2.3.4", "default").getRetryAfterSeconds()).isEqualTo(1L);
    }

    @Test
    void tryConsume_whenWaitIsExactSecond_shouldNotRoundUp() {
        givenProbe(false, 0L, 2_000_000_000L); // đúng 2s

        assertThat(service.tryConsume("1.2.3.4", "default").getRetryAfterSeconds()).isEqualTo(2L);
    }

    @Test
    void rateLimitResult_factories_shouldMapToAllowedAndRejected() {
        assertThat(RateLimitResult.allowed(42L).isAllowed()).isTrue();
        assertThat(RateLimitResult.allowed(42L).getRemainingTokens()).isEqualTo(42L);
        assertThat(RateLimitResult.allowed(42L).getRetryAfterSeconds()).isZero();

        assertThat(RateLimitResult.rejected(7L).isAllowed()).isFalse();
        assertThat(RateLimitResult.rejected(7L).getRemainingTokens()).isZero();
        assertThat(RateLimitResult.rejected(7L).getRetryAfterSeconds()).isEqualTo(7L);
    }

    /**
     * Stub đúng 1 token: nếu RateLimitService gọi tryConsumeAndReturnRemaining với số khác thì
     * stub không khớp, probe trả null và test fail — khoá luôn hành vi "tiêu thụ đúng 1 token".
     */
    private void givenProbe(boolean consumed, long remainingTokens, long nanosToWaitForRefill) {
        when(bucketBuilder.build(anyString(), any(BucketConfiguration.class)).tryConsumeAndReturnRemaining(1))
                .thenReturn(probe);
        when(probe.isConsumed()).thenReturn(consumed);
        if (consumed) {
            when(probe.getRemainingTokens()).thenReturn(remainingTokens);
        } else {
            when(probe.getNanosToWaitForRefill()).thenReturn(nanosToWaitForRefill);
        }
    }
}
