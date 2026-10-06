package com.example.demo.service;

import com.example.demo.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node protection for public authentication endpoints. The key is the
 * normalized e-mail so the response never reveals whether the account exists.
 */
@Service
public class AuthRateLimiter {
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final int loginMaximum;
    private final int resetMaximum;
    private final Duration window;

    public AuthRateLimiter(
            @Value("${api.auth.rate-limit.login-maximum:5}") int loginMaximum,
            @Value("${api.auth.rate-limit.reset-maximum:3}") int resetMaximum,
            @Value("${api.auth.rate-limit.window-seconds:900}") long windowSeconds
    ) {
        this.loginMaximum = loginMaximum;
        this.resetMaximum = resetMaximum;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void checkLogin(String email) {
        check("login", email, loginMaximum);
    }

    public void loginFailed(String email) {
        increment("login", email);
    }

    public void loginSucceeded(String email) {
        attempts.remove(key("login", email));
    }

    public void registerPasswordResetRequest(String email) {
        check("password-reset", email, resetMaximum);
        increment("password-reset", email);
    }

    private void check(String operation, String email, int maximum) {
        AttemptWindow current = attempts.get(key(operation, email));
        if (current == null || expired(current)) {
            if (current != null) {
                attempts.remove(key(operation, email), current);
            }
            return;
        }
        if (current.count >= maximum) {
            throw new RateLimitExceededException();
        }
    }

    private void increment(String operation, String email) {
        String key = key(operation, email);
        attempts.compute(key, (ignored, previous) -> {
            if (previous == null || expired(previous)) {
                return new AttemptWindow(1, Instant.now());
            }
            return new AttemptWindow(previous.count + 1, previous.startedAt);
        });
    }

    private boolean expired(AttemptWindow attempt) {
        return attempt.startedAt.plus(window).isBefore(Instant.now());
    }

    private String key(String operation, String email) {
        return operation + ":" + email.trim().toLowerCase(Locale.ROOT);
    }

    private record AttemptWindow(int count, Instant startedAt) {
    }
}
