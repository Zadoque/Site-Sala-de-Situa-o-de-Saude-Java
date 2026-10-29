package com.example.nss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.nss.DTO.request.FirstAccessRequest;
import com.example.nss.entity.FirstAccessToken;
import com.example.nss.entity.User;
import com.example.nss.repository.FirstAccessTokenRepository;
import com.example.nss.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    user.setAtivo(false);
    FirstAccessToken token = pendingToken(user, "FIRST_ACCESS", "first-access-token");
    when(tokens.findByTokenHash(hash("first-access-token"))).thenReturn(Optional.of(token));
    when(encoder.encode("Password123!")).thenReturn("encoded-password");

    service.complete(
        new FirstAccessRequest("USER@NSS.TEST", "  Nome  ", "Password123!", "first-access-token"));

    assertEquals("Nome", user.getNome());
    assertEquals("encoded-password", user.getPassword());
    org.junit.jupiter.api.Assertions.assertNotNull(user.getPasswordCreatedAt());
    org.junit.jupiter.api.Assertions.assertTrue(user.isAtivo());
    assertEquals("COMPLETED", token.getStatus());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getCompletedAt());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getPasswordCreatedAt());
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

  @Test
  void issuesInvitationAndSendsAUsableLink() {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    MailService mail = mock(MailService.class);
    FirstAccessService service = new FirstAccessService(users, tokens, encoder, mail);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "frontendUrl", "https://app.nss.test/");
    org.springframework.test.util.ReflectionTestUtils.setField(service, "expirationSeconds", 3600L);
    User user = new User();
    user.setId(11L);
    user.setEmail("invite@nss.test");
    FirstAccessToken previous = new FirstAccessToken();
    when(tokens.findByUserId(11L)).thenReturn(Optional.of(previous));

    service.issue(user);

    ArgumentCaptor<FirstAccessToken> captor = ArgumentCaptor.forClass(FirstAccessToken.class);
    org.mockito.Mockito.verify(tokens).save(captor.capture());
    FirstAccessToken token = captor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals("FIRST_ACCESS", token.getPurpose());
    org.junit.jupiter.api.Assertions.assertEquals("PENDING", token.getStatus());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getTokenHash());
    org.junit.jupiter.api.Assertions.assertEquals(user, token.getUser());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getSentAt());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getExpiresAt());
    org.mockito.Mockito.verify(tokens).delete(previous);
    org.mockito.Mockito.verify(mail)
        .sendText(
            org.mockito.ArgumentMatchers.eq("invite@nss.test"),
            org.mockito.ArgumentMatchers.contains("Convite"),
            org.mockito.ArgumentMatchers.contains("primeiro-acesso"));
  }

  @Test
  void issuesPasswordResetWithResetPurposeAndLink() {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    MailService mail = mock(MailService.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class), tokens, mock(PasswordEncoder.class), mail);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "frontendUrl", "https://app.nss.test/");
    User user = new User();
    user.setId(14L);
    user.setEmail("reset@nss.test");
    FirstAccessToken previous = new FirstAccessToken();
    when(tokens.findByUserId(14L)).thenReturn(Optional.of(previous));

    service.issuePasswordReset(user);

    ArgumentCaptor<FirstAccessToken> captor = ArgumentCaptor.forClass(FirstAccessToken.class);
    org.mockito.Mockito.verify(tokens).save(captor.capture());
    FirstAccessToken token = captor.getValue();
    assertEquals("PASSWORD_RESET", token.getPurpose());
    assertEquals("PENDING", token.getStatus());
    assertEquals(user, token.getUser());
    org.junit.jupiter.api.Assertions.assertEquals(64, token.getTokenHash().length());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getSentAt());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getExpiresAt());
    org.mockito.Mockito.verify(tokens).delete(previous);
    org.mockito.Mockito.verify(mail)
        .sendText(
            org.mockito.ArgumentMatchers.eq("reset@nss.test"),
            org.mockito.ArgumentMatchers.contains("Redefinição"),
            org.mockito.ArgumentMatchers.contains("redefinir-senha"));
  }

  @Test
  void pendingReportsOnlyAnUnexpiredPendingToken() throws Exception {
    FirstAccessTokenRepository tokenRepository = mock(FirstAccessTokenRepository.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class),
            tokenRepository,
            mock(PasswordEncoder.class),
            mock(MailService.class));
    User user = new User();
    user.setId(12L);
    FirstAccessToken pending = pendingToken(user, "FIRST_ACCESS", "pending");
    when(tokenRepository.findByUserId(12L)).thenReturn(Optional.of(pending));

    org.junit.jupiter.api.Assertions.assertTrue(service.pending(user));

    pending.setStatus("COMPLETED");
    org.junit.jupiter.api.Assertions.assertFalse(service.pending(user));
  }

  @Test
  void listsAndCancelsPendingInvitations() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class),
            tokens,
            mock(PasswordEncoder.class),
            mock(MailService.class));
    User user = new User();
    user.setId(13L);
    user.setEmail("pending@nss.test");
    user.setAccountType(com.example.nss.entity.AccountType.USER);
    FirstAccessToken token = pendingToken(user, "FIRST_ACCESS", "pending");
    token.setId(13L);
    when(tokens.findByStatusOrderByExpiresAtAsc("PENDING")).thenReturn(List.of(token));
    when(tokens.findById(13L)).thenReturn(Optional.of(token));

    var invitations = service.pendingInvitations();
    service.cancel(13L);

    org.junit.jupiter.api.Assertions.assertEquals(
        "pending@nss.test", invitations.getFirst().email());
    org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", token.getStatus());
    org.mockito.Mockito.verify(tokens).save(token);
  }

  @Test
  void completesPasswordResetAndReactivatesTheUser() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    FirstAccessService service =
        new FirstAccessService(users, tokens, encoder, mock(MailService.class));
    User user = new User();
    user.setEmail("reset@nss.test");
    user.setAtivo(false);
    FirstAccessToken token = pendingToken(user, "PASSWORD_RESET", "reset-token");
    when(tokens.findByTokenHash(hash("reset-token"))).thenReturn(Optional.of(token));
    when(encoder.encode("NewPassword123!")).thenReturn("encoded-reset");

    service.completePasswordReset(
        new com.example.nss.DTO.request.PasswordResetCompleteRequest(
            "RESET@NSS.TEST", "reset-token", "NewPassword123!"));

    assertEquals("encoded-reset", user.getPassword());
    org.junit.jupiter.api.Assertions.assertNotNull(user.getPasswordCreatedAt());
    org.junit.jupiter.api.Assertions.assertTrue(user.isAtivo());
    assertEquals("COMPLETED", token.getStatus());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getCompletedAt());
    org.junit.jupiter.api.Assertions.assertNotNull(token.getPasswordCreatedAt());
    org.mockito.Mockito.verify(users).save(user);
    org.mockito.Mockito.verify(tokens).save(token);
  }

  @Test
  void rejectsExpiredPasswordReset() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class),
            tokens,
            mock(PasswordEncoder.class),
            mock(MailService.class));
    User user = new User();
    user.setEmail("reset@nss.test");
    FirstAccessToken token = pendingToken(user, "PASSWORD_RESET", "expired-reset");
    token.setExpiresAt(Instant.now().minusSeconds(1));
    when(tokens.findByTokenHash(hash("expired-reset"))).thenReturn(Optional.of(token));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.completePasswordReset(
                new com.example.nss.DTO.request.PasswordResetCompleteRequest(
                    "reset@nss.test", "expired-reset", "NewPassword123!")));
  }

  @Test
  void renewsPendingInvitationByCancellingTheOldToken() throws Exception {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    MailService mail = mock(MailService.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class), tokens, mock(PasswordEncoder.class), mail);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "frontendUrl", "https://app.nss.test");
    User user = new User();
    user.setId(22L);
    user.setEmail("renew@nss.test");
    FirstAccessToken old = pendingToken(user, "FIRST_ACCESS", "old-token");
    old.setId(22L);
    FirstAccessToken replacement = pendingToken(user, "FIRST_ACCESS", "replacement-token");
    replacement.setId(23L);
    when(tokens.findById(22L)).thenReturn(Optional.of(old));
    when(tokens.findByUserId(22L)).thenReturn(Optional.empty(), Optional.of(replacement));

    var invitation = service.renew(22L);

    assertEquals("CANCELLED", old.getStatus());
    assertEquals(23L, invitation.id());
    org.mockito.Mockito.verify(tokens).save(old);
    org.mockito.Mockito.verify(mail)
        .sendText(
            org.mockito.ArgumentMatchers.eq("renew@nss.test"),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void doesNotRenewAnAlreadyCompletedInvitation() {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class),
            tokens,
            mock(PasswordEncoder.class),
            mock(MailService.class));
    FirstAccessToken token = new FirstAccessToken();
    token.setStatus("COMPLETED");
    when(tokens.findById(30L)).thenReturn(Optional.of(token));

    assertThrows(IllegalArgumentException.class, () -> service.renew(30L));
    org.mockito.Mockito.verify(tokens, org.mockito.Mockito.never()).save(token);
  }

  @Test
  void cancellingAnAlreadyCompletedInvitationDoesNotPersistAChange() {
    FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    FirstAccessService service =
        new FirstAccessService(
            mock(UserRepository.class),
            tokens,
            mock(PasswordEncoder.class),
            mock(MailService.class));
    FirstAccessToken token = new FirstAccessToken();
    token.setStatus("COMPLETED");
    when(tokens.findById(31L)).thenReturn(Optional.of(token));

    service.cancel(31L);

    org.mockito.Mockito.verify(tokens, org.mockito.Mockito.never()).save(token);
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
