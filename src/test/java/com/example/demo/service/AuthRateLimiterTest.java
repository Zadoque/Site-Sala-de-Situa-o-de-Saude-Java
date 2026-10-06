package com.example.demo.service;

import com.example.demo.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimiterTest {
    @Test
    void blocksTheSixthFailedLoginWithinTheWindowAndClearsOnSuccess() {
        AuthRateLimiter limiter = new AuthRateLimiter(5, 3, 900);

        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.checkLogin("user@nss.test");
            limiter.loginFailed("user@nss.test");
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.checkLogin("USER@nss.test"));

        limiter.loginSucceeded("user@nss.test");
        assertDoesNotThrow(() -> limiter.checkLogin("user@nss.test"));
    }

    @Test
    void limitsPasswordResetRequestsWithoutDependingOnAccountExistence() {
        AuthRateLimiter limiter = new AuthRateLimiter(5, 3, 900);

        for (int attempt = 0; attempt < 3; attempt++) {
            limiter.registerPasswordResetRequest("unknown@nss.test");
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.registerPasswordResetRequest("unknown@nss.test"));
    }
}
