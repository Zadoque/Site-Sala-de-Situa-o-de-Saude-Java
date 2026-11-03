package com.example.nss.user.application.port;

import com.example.nss.user.domain.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FirstAccessGateway {
  Optional<TokenData> findByUserId(Long userId);

  Optional<TokenData> findByTokenHashForUpdate(String tokenHash);

  Optional<TokenData> findById(Long id);

  List<TokenData> findByStatusOrderByExpiresAt(String status);

  TokenData save(TokenData token);

  void delete(TokenData token);

  void flush();

  final class TokenData {
    private Long id;
    private UserAccount user;
    private String tokenHash;
    private String status;
    private String purpose;
    private Instant expiresAt;
    private Instant sentAt;
    private Instant completedAt;
    private Instant passwordCreatedAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public UserAccount getUser() {
      return user;
    }

    public void setUser(UserAccount user) {
      this.user = user;
    }

    public String getTokenHash() {
      return tokenHash;
    }

    public void setTokenHash(String value) {
      tokenHash = value;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String value) {
      status = value;
    }

    public String getPurpose() {
      return purpose;
    }

    public void setPurpose(String value) {
      purpose = value;
    }

    public Instant getExpiresAt() {
      return expiresAt;
    }

    public void setExpiresAt(Instant value) {
      expiresAt = value;
    }

    public Instant getSentAt() {
      return sentAt;
    }

    public void setSentAt(Instant value) {
      sentAt = value;
    }

    public Instant getCompletedAt() {
      return completedAt;
    }

    public void setCompletedAt(Instant value) {
      completedAt = value;
    }

    public Instant getPasswordCreatedAt() {
      return passwordCreatedAt;
    }

    public void setPasswordCreatedAt(Instant value) {
      passwordCreatedAt = value;
    }
  }
}
