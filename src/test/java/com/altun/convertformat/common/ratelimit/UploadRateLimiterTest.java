package com.altun.convertformat.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadRateLimiterTest {

    @Test
    void rejectsRequestsOverClientLimit() {
        UploadRateLimiter rateLimiter = new UploadRateLimiter(
                2,
                Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-09-13T00:00:00Z"), ZoneOffset.UTC)
        );

        assertTrue(rateLimiter.tryAcquire("127.0.0.1"));
        assertTrue(rateLimiter.tryAcquire("127.0.0.1"));
        assertFalse(rateLimiter.tryAcquire("127.0.0.1"));
        assertTrue(rateLimiter.tryAcquire("127.0.0.2"));
    }
}
