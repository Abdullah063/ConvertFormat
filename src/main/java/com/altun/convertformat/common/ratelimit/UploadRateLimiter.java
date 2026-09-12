package com.altun.convertformat.common.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class UploadRateLimiter {

    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final int requestLimit;
    private final Duration windowDuration;
    private final Clock clock;
    private final ConcurrentMap<String, RequestWindow> windows = new ConcurrentHashMap<>();

    public UploadRateLimiter(
            @Value("${app.rate-limit.upload.requests:10}") int requestLimit,
            @Value("${app.rate-limit.upload.window:1m}") Duration windowDuration,
            Clock clock
    ) {
        if (requestLimit < 1 || windowDuration.isZero() || windowDuration.isNegative()) {
            throw new IllegalArgumentException("Rate limit ayarları pozitif olmalıdır");
        }
        this.requestLimit = requestLimit;
        this.windowDuration = windowDuration;
        this.clock = clock;
    }

    public boolean tryAcquire(String clientKey) {
        long currentWindow = clock.millis() / windowDuration.toMillis();
        removeExpiredWindows(currentWindow);

        if (!windows.containsKey(clientKey) && windows.size() >= MAX_TRACKED_CLIENTS) {
            return false;
        }

        RequestWindow updated = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || existing.windowNumber() != currentWindow) {
                return new RequestWindow(currentWindow, 1);
            }
            return existing.increment();
        });

        return updated.requestCount() <= requestLimit;
    }

    public long retryAfterSeconds() {
        return Math.max(1, windowDuration.toSeconds());
    }

    private void removeExpiredWindows(long currentWindow) {
        if (windows.size() < MAX_TRACKED_CLIENTS) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().windowNumber() != currentWindow);
    }

    private record RequestWindow(long windowNumber, int requestCount) {

        private RequestWindow increment() {
            return new RequestWindow(windowNumber, requestCount + 1);
        }
    }
}
