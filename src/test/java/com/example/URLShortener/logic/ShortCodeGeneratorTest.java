package com.example.URLShortener.logic;

import com.example.URLShortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock Base62Encoder encoder;
    @Mock CounterPermutation permutation;
    @Mock CounterAllocator allocator;
    @Mock ShortUrlRepository repository;

    @Test
    void next_whenCandidateIsAvailable_shouldReturnEncodedCandidate() {
        when(allocator.next()).thenReturn(10L);
        when(permutation.permute(10L)).thenReturn(20L);
        when(encoder.encode(20L)).thenReturn("abc123");
        when(repository.existsByShortCode("abc123")).thenReturn(false);

        ShortCodeGenerator generator = new ShortCodeGenerator(encoder, allocator, repository, permutation);

        assertThat(generator.next()).isEqualTo("abc123");
        verify(allocator).next();
        verify(repository).existsByShortCode("abc123");
    }

    @Test
    void next_whenCandidateCollides_shouldRetryUntilAvailable() {
        when(allocator.next()).thenReturn(10L, 11L);
        when(permutation.permute(10L)).thenReturn(20L);
        when(permutation.permute(11L)).thenReturn(21L);
        when(encoder.encode(20L)).thenReturn("used");
        when(encoder.encode(21L)).thenReturn("free");
        when(repository.existsByShortCode("used")).thenReturn(true);
        when(repository.existsByShortCode("free")).thenReturn(false);

        ShortCodeGenerator generator = new ShortCodeGenerator(encoder, allocator, repository, permutation);

        assertThat(generator.next()).isEqualTo("free");
        verify(allocator, times(2)).next();
    }
}
