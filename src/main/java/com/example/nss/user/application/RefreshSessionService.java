package com.example.nss.user.application;

import com.example.nss.user.application.port.RefreshSessionGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {
  private final RefreshSessionGateway sessions;
  private final UserGateway users;
  private final SecureRandom random = new SecureRandom();

  @Value("${api.auth.refresh-expiration:604800000}")
  private long expirationMillis;

  @Transactional
  public String issue(UserAccount user) {
    UserAccount managed =
        users
            .findById(user.getId())
            .orElseThrow(() -> new BadCredentialsException("Conta não encontrada"));
    if (!managed.isAtivo()) throw new BadCredentialsException("Conta desativada");
    String raw = randomToken();
    sessions.save(newSession(managed, raw));
    return raw;
  }

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public Rotation rotateWithToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank())
      throw new BadCredentialsException("Refresh inválido");
    RefreshSessionGateway.SessionData current =
        sessions
            .findByTokenHashForUpdate(hash(rawToken))
            .orElseThrow(() -> new BadCredentialsException("Refresh inválido"));
    Instant now = Instant.now();
    if (current.getRevokedAt() != null
        || !current.getExpiresAt().isAfter(now)
        || !current.getUser().isAtivo()) {
      if (current.getRevokedAt() == null) {
        current.setRevokedAt(now);
        sessions.save(current);
      }
      throw new BadCredentialsException("Refresh inválido");
    }
    String replacementRaw = randomToken();
    RefreshSessionGateway.SessionData replacement = newSession(current.getUser(), replacementRaw);
    sessions.save(replacement);
    current.setRevokedAt(now);
    current.setReplacedBySessionId(replacement.getId());
    sessions.save(current);
    return new Rotation(current.getUser(), replacementRaw);
  }

  @Transactional
  public void revoke(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    sessions
        .findByTokenHashForUpdate(hash(rawToken))
        .ifPresent(
            session -> {
              if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
                sessions.save(session);
              }
            });
  }

  @Transactional
  public void revokeAllForUser(Long userId) {
    sessions.revokeActiveByUserId(userId, Instant.now());
  }

  private RefreshSessionGateway.SessionData newSession(UserAccount user, String rawToken) {
    RefreshSessionGateway.SessionData session = new RefreshSessionGateway.SessionData();
    session.setId(UUID.randomUUID());
    session.setUser(user);
    session.setTokenHash(hash(rawToken));
    session.setCreatedAt(Instant.now());
    session.setExpiresAt(Instant.now().plus(Duration.ofMillis(expirationMillis)));
    return session;
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Não foi possível proteger o refresh token", e);
    }
  }

  public record Rotation(UserAccount user, String rawToken) {
    public String email() {
      return user.getEmail();
    }
  }
}
