package com.example.nss.user.application;

import com.example.nss.user.application.port.FirstAccessGateway;
import com.example.nss.user.application.port.RefreshSessionGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.infrastructure.persistence.FirstAccessToken;
import com.example.nss.user.infrastructure.persistence.RefreshSession;
import com.example.nss.user.infrastructure.persistence.User;
import com.example.nss.user.infrastructure.repository.FirstAccessTokenRepository;
import com.example.nss.user.infrastructure.repository.RefreshSessionRepository;
import com.example.nss.user.infrastructure.repository.UserRepository;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Optional;

public final class LegacyUserTestAdapters {
  static UserGateway users(UserRepository repository) {
    return new Users(repository);
  }

  static FirstAccessGateway tokens(FirstAccessTokenRepository repository) {
    return new Tokens(repository);
  }

  static RefreshSessionGateway sessions(RefreshSessionRepository repository) {
    return new Sessions(repository);
  }

  public static UserAccount domain(User source) {
    UserAccount target = new UserAccount();
    target.setId(source.getId());
    target.setNome(source.getNome());
    target.setEmail(source.getEmail());
    target.setPassword(source.getPassword());
    target.setAccountType(source.getAccountType());
    target.setAtivo(source.isAtivo());
    target.setCreatedAt(source.getCreatedAt());
    target.setUpdatedAt(source.getUpdatedAt());
    target.setPasswordCreatedAt(source.getPasswordCreatedAt());
    return target;
  }

  private static final class Users implements UserGateway {
    private final UserRepository repository;
    private final IdentityHashMap<UserAccount, User> originals = new IdentityHashMap<>();

    Users(UserRepository repository) {
      this.repository = repository;
    }

    public Optional<UserAccount> findByEmail(String email) {
      return repository.findByEmailIgnoreCase(email).map(this::remember);
    }

    public Optional<UserAccount> findById(Long id) {
      return repository.findById(id).map(this::remember);
    }

    public UserAccount save(UserAccount account) {
      User original = originals.get(account);
      if (original == null && account.getId() != null)
        original = repository.findById(account.getId()).orElse(null);
      if (original == null && account.getEmail() != null)
        original = repository.findByEmailIgnoreCase(account.getEmail()).orElse(null);
      User candidate = original == null ? entity(account) : copy(account, original);
      User saved = repository.save(candidate);
      return remember(saved == null ? candidate : saved);
    }

    public Page search(String search, int page, int size) {
      return new Page(java.util.List.of(), page, size, false, 0);
    }

    private UserAccount remember(User source) {
      UserAccount account = domain(source);
      originals.put(account, source);
      return account;
    }

    private User entity(UserAccount account) {
      User target = new User();
      target.setId(account.getId());
      target.setNome(account.getNome());
      target.setEmail(account.getEmail());
      target.setPassword(account.getPassword());
      target.setAccountType(account.getAccountType());
      target.setAtivo(account.isAtivo());
      target.setCreatedAt(account.getCreatedAt());
      target.setUpdatedAt(account.getUpdatedAt());
      target.setPasswordCreatedAt(account.getPasswordCreatedAt());
      return target;
    }

    private User copy(UserAccount account, User target) {
      target.setId(account.getId());
      target.setNome(account.getNome());
      target.setEmail(account.getEmail());
      target.setPassword(account.getPassword());
      target.setAccountType(account.getAccountType());
      target.setAtivo(account.isAtivo());
      target.setCreatedAt(account.getCreatedAt());
      target.setUpdatedAt(account.getUpdatedAt());
      target.setPasswordCreatedAt(account.getPasswordCreatedAt());
      return target;
    }
  }

  private static final class Tokens implements FirstAccessGateway {
    private final FirstAccessTokenRepository repository;
    private final IdentityHashMap<TokenData, FirstAccessToken> originals = new IdentityHashMap<>();

    Tokens(FirstAccessTokenRepository repository) {
      this.repository = repository;
    }

    public Optional<TokenData> findByUserId(Long id) {
      return repository.findByUserId(id).map(this::data);
    }

    public Optional<TokenData> findByTokenHashForUpdate(String hash) {
      return repository.findByTokenHashForUpdate(hash).map(this::data);
    }

    public Optional<TokenData> findById(Long id) {
      return repository.findById(id).map(this::data);
    }

    public java.util.List<TokenData> findByStatusOrderByExpiresAt(String status) {
      return repository.findByStatusOrderByExpiresAtAsc(status).stream().map(this::data).toList();
    }

    public TokenData save(TokenData token) {
      FirstAccessToken original = originals.get(token);
      FirstAccessToken candidate = copy(token, original);
      FirstAccessToken saved = repository.save(candidate);
      return data(saved == null ? candidate : saved);
    }

    public void delete(TokenData token) {
      repository.delete(originals.getOrDefault(token, copy(token, null)));
    }

    public void flush() {
      repository.flush();
    }

    private TokenData data(FirstAccessToken source) {
      TokenData target = new TokenData();
      target.setId(source.getId());
      if (source.getUser() != null) target.setUser(domain(source.getUser()));
      else target.setUser(new UserAccount());
      target.setTokenHash(source.getTokenHash());
      target.setStatus(source.getStatus());
      target.setPurpose(source.getPurpose());
      target.setExpiresAt(source.getExpiresAt());
      target.setSentAt(source.getSentAt());
      target.setCompletedAt(source.getCompletedAt());
      target.setPasswordCreatedAt(source.getPasswordCreatedAt());
      originals.put(target, source);
      return target;
    }

    private FirstAccessToken copy(TokenData source, FirstAccessToken target) {
      if (target == null) target = new FirstAccessToken();
      target.setId(source.getId());
      User user = new User();
      UserAccount account = source.getUser();
      user.setId(account.getId());
      user.setEmail(account.getEmail());
      user.setNome(account.getNome());
      user.setAtivo(account.isAtivo());
      user.setAccountType(account.getAccountType());
      user.setPassword(account.getPassword());
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
  }

  private static final class Sessions implements RefreshSessionGateway {
    private final RefreshSessionRepository repository;
    private final IdentityHashMap<SessionData, RefreshSession> originals = new IdentityHashMap<>();

    Sessions(RefreshSessionRepository repository) {
      this.repository = repository;
    }

    public Optional<SessionData> findByTokenHashForUpdate(String hash) {
      return repository.findByTokenHashForUpdate(hash).map(this::data);
    }

    public SessionData save(SessionData source) {
      RefreshSession candidate = originals.get(source);
      if (candidate == null) candidate = entity(source);
      else update(source, candidate);
      RefreshSession saved = repository.save(candidate);
      return data(saved == null ? candidate : saved);
    }

    public void revokeActiveByUserId(Long id, Instant at) {
      repository.revokeActiveByUserId(id, at);
    }

    private SessionData data(RefreshSession source) {
      SessionData target = new SessionData();
      target.setId(source.getId());
      target.setUser(source.getUser() == null ? new UserAccount() : domain(source.getUser()));
      target.setTokenHash(source.getTokenHash());
      target.setCreatedAt(source.getCreatedAt());
      target.setExpiresAt(source.getExpiresAt());
      target.setRevokedAt(source.getRevokedAt());
      target.setReplacedBySessionId(source.getReplacedBySessionId());
      originals.put(target, source);
      return target;
    }

    private void update(SessionData source, RefreshSession target) {
      target.setRevokedAt(source.getRevokedAt());
      target.setReplacedBySessionId(source.getReplacedBySessionId());
      target.setExpiresAt(source.getExpiresAt());
    }

    private RefreshSession entity(SessionData source) {
      RefreshSession target = new RefreshSession();
      target.setId(source.getId());
      User user = new User();
      user.setId(source.getUser().getId());
      user.setEmail(source.getUser().getEmail());
      user.setAtivo(source.getUser().isAtivo());
      user.setAccountType(source.getUser().getAccountType());
      target.setUser(user);
      target.setTokenHash(source.getTokenHash());
      target.setCreatedAt(source.getCreatedAt());
      target.setExpiresAt(source.getExpiresAt());
      target.setRevokedAt(source.getRevokedAt());
      target.setReplacedBySessionId(source.getReplacedBySessionId());
      return target;
    }
  }
}
