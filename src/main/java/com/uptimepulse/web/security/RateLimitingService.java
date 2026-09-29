package com.uptimepulse.web.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    private static class AttemptTracker {
        int attempts = 0;
        long lastAttemptTime = System.currentTimeMillis();

        boolean isBlocked() {
            if (attempts >= MAX_ATTEMPTS) {
                if (System.currentTimeMillis() - lastAttemptTime < LOCKOUT_DURATION_MS) {
                    return true;
                } else {
                    // Lockout expired, reset
                    attempts = 0;
                    return false;
                }
            }
            return false;
        }

        void recordFailure() {
            attempts++;
            lastAttemptTime = System.currentTimeMillis();
        }

        void reset() {
            attempts = 0;
        }

        long getRemainingCooldownSeconds() {
            long remaining = LOCKOUT_DURATION_MS - (System.currentTimeMillis() - lastAttemptTime);
            return Math.max(0, remaining / 1000);
        }
    }

    private final Map<String, AttemptTracker> attemptsMap = new ConcurrentHashMap<>();

    public void checkRateLimit(String key) {
        AttemptTracker tracker = attemptsMap.get(key);
        if (tracker != null && tracker.isBlocked()) {
            throw new IllegalArgumentException("Too many failed attempts. Please wait " 
                    + tracker.getRemainingCooldownSeconds() + " seconds before trying again.");
        }
    }

    public void recordFailedAttempt(String key) {
        attemptsMap.computeIfAbsent(key, k -> new AttemptTracker()).recordFailure();
    }

    public void recordSuccess(String key) {
        attemptsMap.remove(key);
    }
}
