package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(
            "ZmFrZS10ZXN0LXNlY3JldC1sb25nLWVub3VnaC1mb3ItaHMyNTYtMzI=",
            60_000
    );

    @Test
    void acceptsAnEnabledUserBoundToTheToken() {
        UserDetails user = User.withUsername("user@nss.test").password("ignored").roles("USER").build();

        assertTrue(jwt.isTokenValid(jwt.generateToken(user), user));
    }

    @Test
    void rejectsAPreviouslyIssuedTokenWhenTheUserIsDisabled() {
        UserDetails enabled = User.withUsername("user@nss.test").password("ignored").roles("USER").build();
        UserDetails disabled = User.withUsername("user@nss.test").password("ignored").roles("USER").disabled(true).build();

        assertFalse(jwt.isTokenValid(jwt.generateToken(enabled), disabled));
    }

    @Test
    void rejectsASecretThatIsNotBase64() {
        assertThrows(IllegalStateException.class, () -> new JwtService("not-a-base64-secret", 60_000));
    }
}
