package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.DTO.request.FirstAccessRequest;
import com.example.demo.entity.FirstAccessToken;
import com.example.demo.entity.User;
import com.example.demo.repository.FirstAccessTokenRepository;
import com.example.demo.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class FirstAccessServiceTest {
  @Test
  void completesFirstAccessAndConsumesTheInvitation() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    MailService mail = mock(MailService.class);
    FirstAccessService service = new FirstAccessService(users, tokens, encoder, mail);
    User user = new User();
    user.setEmail("user@nss.test");
    FirstAccessToken token = pendingToken(user, "FIRST_ACCESS", "first-access-token");
    when(tokens.findByTokenHash(hash("first-access-token"))).thenReturn(Optional.of(token));
    when(encoder.encode("Password123!")).thenReturn("encoded-password");

    service.complete(
        new FirstAccessRequest("USER@NSS.TEST", "  Nome  ", "Password123!", "first-access-token"));

    assertEquals("Nome", user.getNome());
    assertEquals("encoded-password", user.getPassword());
    assertEquals("COMPLETED", token.getStatus());
    org.mockito.Mockito.verify(users).save(user);
    org.mockito.Mockito.verify(tokens).save(token);
  }

  @Test
  void rejectsFirstAccessWhenEmailDoesNotMatchTheInvitation() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    MailService mail = mock(MailService.class);
    FirstAccessService service = new FirstAccessService(users, tokens, encoder, mail);
    User user = new User();
    user.setEmail("user@nss.test");
    FirstAccessToken token = pendingToken(user, "FIRST_ACCESS", "first-access-token");
    when(tokens.findByTokenHash(hash("first-access-token"))).thenReturn(Optional.of(token));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.complete(
                new FirstAccessRequest(
                    "other@nss.test", "Nome", "Password123!", "first-access-token")));
  }

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
    String hash =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    token.setTokenHash(hash);
    when(tokens.findByTokenHash(hash)).thenReturn(Optional.of(token));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.complete(new FirstAccessRequest("user@nss.test", "Nome", "Password123!", raw)));
  }

  private static FirstAccessToken pendingToken(User user, String purpose, String raw)
      throws Exception {
    FirstAccessToken token = new FirstAccessToken();
    token.setUser(user);
    token.setTokenHash(hash(raw));
    token.setStatus("PENDING");
    token.setPurpose(purpose);
    token.setExpiresAt(Instant.now().plusSeconds(60));
    return token;
  }

  private static String hash(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
