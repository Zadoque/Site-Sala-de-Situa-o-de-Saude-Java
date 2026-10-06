package com.example.demo.service;

import com.example.demo.DTO.request.FirstAccessRequest;
import com.example.demo.entity.FirstAccessToken;
import com.example.demo.entity.User;
import com.example.demo.repository.FirstAccessTokenRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirstAccessServiceTest {
    @Test
    void firstAccessCannotConsumeAPasswordResetToken() throws Exception {
        FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        MailService mail = mock(MailService.class);
        FirstAccessService service = new FirstAccessService(users, tokens, encoder, mail);
        User user = new User();
        user.setEmail("user@nss.test");
        FirstAccessToken token = new FirstAccessToken();
        token.setUser(user);
        token.setStatus("PENDING");
        token.setPurpose("PASSWORD_RESET");
        token.setExpiresAt(Instant.now().plusSeconds(60));
        String raw = "token-only-for-test";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        when(tokens.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class,
                () -> service.complete(new FirstAccessRequest("user@nss.test", "Nome", "Password123!", raw)));
    }
}
