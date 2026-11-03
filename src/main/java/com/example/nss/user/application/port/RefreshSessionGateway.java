package com.example.nss.user.application.port;

import com.example.nss.user.domain.UserAccount;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionGateway {
  Optional<SessionData> findByTokenHashForUpdate(String hash);

  SessionData save(SessionData session);

  void revokeActiveByUserId(Long userId, Instant at);

  final class SessionData {
    private UUID id;
    private UserAccount user;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedBySessionId;

    public UUID getId() {
      return id;
    }

    public void setId(UUID v) {
      id = v;
    }

    public UserAccount getUser() {
      return user;
    }

    public void setUser(UserAccount v) {
      user = v;
    }

    public String getTokenHash() {
      return tokenHash;
    }

    public void setTokenHash(String v) {
      tokenHash = v;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant v) {
      createdAt = v;
    }

    public Instant getExpiresAt() {
      return expiresAt;
    }

    public void setExpiresAt(Instant v) {
      expiresAt = v;
    }

    public Instant getRevokedAt() {
      return revokedAt;
    }

    public void setRevokedAt(Instant v) {
      revokedAt = v;
    }

    public UUID getReplacedBySessionId() {
      return replacedBySessionId;
    }

    public void setReplacedBySessionId(UUID v) {
      replacedBySessionId = v;
    }
  }
}
