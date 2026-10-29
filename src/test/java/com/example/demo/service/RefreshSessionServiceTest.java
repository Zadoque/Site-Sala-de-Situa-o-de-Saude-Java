package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.entity.RefreshSession;
import com.example.demo.entity.User;
import com.example.demo.repository.RefreshSessionRepository;
import com.example.demo.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class RefreshSessionServiceTest {
  @Test
  void issuesARefreshTokenForAnActiveManagedUser() {
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    UserRepository users = mock(UserRepository.class);
    RefreshSessionService service = new RefreshSessionService(sessions, users);
    setExpiration(service, 60_000L);
    User user = new User();
    user.setId(7L);
    user.setAtivo(true);
    when(users.findById(7L)).thenReturn(java.util.Optional.of(user));

    String token = service.issue(user);

    assertNotNull(token);
    verify(sessions).save(any(RefreshSession.class));
  }

  @Test
  void rotatesAnActiveRefreshTokenAndRevokesThePreviousSession() {
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    UserRepository users = mock(UserRepository.class);
    RefreshSessionService service = new RefreshSessionService(sessions, users);
    setExpiration(service, 60_000L);
    User user = new User();
    user.setAtivo(true);
    RefreshSession current = new RefreshSession();
    current.setId(UUID.randomUUID());
    current.setUser(user);
    current.setExpiresAt(Instant.now().plusSeconds(60));
    when(sessions.findByTokenHashForUpdate(any())).thenReturn(java.util.Optional.of(current));

    RefreshSessionService.Rotation rotation = service.rotateWithToken("opaque-refresh");

    assertEquals(user, rotation.user());
    assertNotNull(rotation.rawToken());
    assertNotNull(current.getRevokedAt());
    assertNotNull(current.getReplacedBySessionId());
    verify(sessions, times(2)).save(any(RefreshSession.class));
  }

  @Test
  void rejectsBlankOrUnknownRefreshTokens() {
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    UserRepository users = mock(UserRepository.class);
    RefreshSessionService service = new RefreshSessionService(sessions, users);
    when(sessions.findByTokenHashForUpdate(any())).thenReturn(java.util.Optional.empty());

    assertThrows(BadCredentialsException.class, () -> service.rotateWithToken(" "));
    assertThrows(BadCredentialsException.class, () -> service.rotateWithToken("unknown"));
  }

  @Test
  void rejectsRefreshForAnInactiveUserAndRevokesTheStoredSession() {
    RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    UserRepository users = mock(UserRepository.class);
    RefreshSessionService service = new RefreshSessionService(sessions, users);
    setExpiration(service, 60_000L);
    User user = new User();
    user.setAtivo(false);
    RefreshSession session = new RefreshSession();
    session.setUser(user);
    session.setExpiresAt(Instant.now().plusSeconds(60));
    when(sessions.findByTokenHashForUpdate(any())).thenReturn(Optional.of(session));

    assertThrows(BadCredentialsException.class, () -> service.rotateWithToken("opaque-refresh"));
    verify(sessions).save(session);
  }

  private void setExpiration(RefreshSessionService service, long value) {
    try {
      var field = RefreshSessionService.class.getDeclaredField("expirationMillis");
      field.setAccessible(true);
      field.set(service, value);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError(exception);
    }
  }
}
