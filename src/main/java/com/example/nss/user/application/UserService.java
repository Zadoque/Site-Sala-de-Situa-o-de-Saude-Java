package com.example.nss.user.application;

import com.example.nss.user.infrastructure.persistence.User;
import com.example.nss.user.infrastructure.repository.UserRepository;
import com.example.nss.user.security.EmailNormalizer;
import com.example.nss.user.application.port.UserMapperPort;
import com.example.nss.user.application.query.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repository;
  private final FirstAccessService firstAccessService;
  private final UserMapperPort mapper;

  @SuppressWarnings("unchecked")
  public <T extends UserView> T getByEmail(String email) {
    return (T)
        repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .map(mapper::toResponse)
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  @Transactional(readOnly = true)
  public User entityByEmail(String email) {
    return repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  public void requestPasswordReset(String email) {
    repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .filter(User::isAtivo)
        .ifPresent(firstAccessService::issuePasswordReset);
  }
}
