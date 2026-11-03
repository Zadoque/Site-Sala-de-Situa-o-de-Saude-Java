package com.example.nss.user.infrastructure.repository;

import com.example.nss.user.application.port.FirstAccessGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.infrastructure.persistence.FirstAccessToken;
import com.example.nss.user.infrastructure.persistence.User;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirstAccessGatewayAdapter implements FirstAccessGateway {
  private final FirstAccessTokenRepository repository;
  private final UserGateway users;

  @Override
  public Optional<TokenData> findByUserId(Long id) {
    return repository.findByUserId(id).map(this::toData);
  }

  @Override
  public Optional<TokenData> findByTokenHashForUpdate(String hash) {
    return repository.findByTokenHashForUpdate(hash).map(this::toData);
  }

  @Override
  public Optional<TokenData> findById(Long id) {
    return repository.findById(id).map(this::toData);
  }

  @Override
  public List<TokenData> findByStatusOrderByExpiresAt(String status) {
    return repository.findByStatusOrderByExpiresAtAsc(status).stream().map(this::toData).toList();
  }

  @Override
  public TokenData save(TokenData token) {
    return toData(repository.save(toEntity(token)));
  }

  @Override
  public void delete(TokenData token) {
    repository.delete(toEntity(token));
  }

  @Override
  public void flush() {
    repository.flush();
  }

  private TokenData toData(FirstAccessToken source) {
    TokenData target = new TokenData();
    target.setId(source.getId());
    target.setUser(toDomain(source.getUser()));
    target.setTokenHash(source.getTokenHash());
    target.setStatus(source.getStatus());
    target.setPurpose(source.getPurpose());
    target.setExpiresAt(source.getExpiresAt());
    target.setSentAt(source.getSentAt());
    target.setCompletedAt(source.getCompletedAt());
    target.setPasswordCreatedAt(source.getPasswordCreatedAt());
    return target;
  }

  private FirstAccessToken toEntity(TokenData source) {
    FirstAccessToken target = new FirstAccessToken();
    target.setId(source.getId());
    User user = new User();
    UserAccount account = source.getUser();
    user.setId(account.getId());
    user.setEmail(account.getEmail());
    user.setNome(account.getNome());
    user.setAtivo(account.isAtivo());
    user.setAccountType(account.getAccountType());
    user.setPassword(account.getPassword());
    user.setCreatedAt(account.getCreatedAt());
    user.setUpdatedAt(account.getUpdatedAt());
    user.setPasswordCreatedAt(account.getPasswordCreatedAt());
    target.setUser(user);
    target.setTokenHash(source.getTokenHash());
    target.setStatus(source.getStatus());
    target.setPurpose(source.getPurpose());
    target.setExpiresAt(source.getExpiresAt());
    target.setSentAt(source.getSentAt());
    target.setCompletedAt(source.getCompletedAt());
    target.setPasswordCreatedAt(source.getPasswordCreatedAt());
    return target;
  }

  private UserAccount toDomain(User source) {
    return users
        .findById(source.getId())
        .orElseGet(
            () -> {
              UserAccount account = new UserAccount();
              account.setId(source.getId());
              account.setNome(source.getNome());
              account.setEmail(source.getEmail());
              account.setPassword(source.getPassword());
              account.setAccountType(source.getAccountType());
              account.setAtivo(source.isAtivo());
              account.setCreatedAt(source.getCreatedAt());
              account.setUpdatedAt(source.getUpdatedAt());
              account.setPasswordCreatedAt(source.getPasswordCreatedAt());
              return account;
            });
  }
}
