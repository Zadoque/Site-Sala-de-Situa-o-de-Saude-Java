package com.example.nss.user.infrastructure.repository;

import com.example.nss.user.application.port.RefreshSessionGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.infrastructure.persistence.RefreshSession;
import com.example.nss.user.infrastructure.persistence.User;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshSessionGatewayAdapter implements RefreshSessionGateway {
  private final RefreshSessionRepository repository;
  private final UserGateway users;

  @Override
  public Optional<SessionData> findByTokenHashForUpdate(String hash) {
    return repository.findByTokenHashForUpdate(hash).map(this::toData);
  }

  @Override
  public SessionData save(SessionData data) {
    return toData(repository.save(toEntity(data)));
  }

  @Override
  public void revokeActiveByUserId(Long id, Instant at) {
    repository.revokeActiveByUserId(id, at);
  }

  private SessionData toData(RefreshSession source) {
    SessionData target = new SessionData();
    target.setId(source.getId());
    target.setUser(toDomain(source.getUser()));
    target.setTokenHash(source.getTokenHash());
    target.setCreatedAt(source.getCreatedAt());
    target.setExpiresAt(source.getExpiresAt());
    target.setRevokedAt(source.getRevokedAt());
    target.setReplacedBySessionId(source.getReplacedBySessionId());
    return target;
  }

  private RefreshSession toEntity(SessionData source) {
    RefreshSession target = new RefreshSession();
    target.setId(source.getId());
    User user = new User();
    UserAccount account = source.getUser();
    user.setId(account.getId());
    user.setEmail(account.getEmail());
    user.setAtivo(account.isAtivo());
    user.setAccountType(account.getAccountType());
    target.setUser(user);
    target.setTokenHash(source.getTokenHash());
    target.setCreatedAt(source.getCreatedAt());
    target.setExpiresAt(source.getExpiresAt());
    target.setRevokedAt(source.getRevokedAt());
    target.setReplacedBySessionId(source.getReplacedBySessionId());
    return target;
  }

  private UserAccount toDomain(User source) {
    return users
        .findById(source.getId())
        .orElseGet(
            () -> {
              UserAccount a = new UserAccount();
              a.setId(source.getId());
              a.setEmail(source.getEmail());
              a.setAtivo(source.isAtivo());
              a.setAccountType(source.getAccountType());
              return a;
            });
  }
}
