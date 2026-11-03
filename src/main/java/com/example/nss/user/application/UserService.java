package com.example.nss.user.application;

import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.application.port.UserMapperPort;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.domain.rule.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserGateway repository;
  private final FirstAccessService firstAccessService;
  private final UserMapperPort mapper;

  @SuppressWarnings("unchecked")
  public <T extends UserView> T getByEmail(String email) {
    return (T)
        repository
            .findByEmail(EmailNormalizer.normalize(email))
            .map(mapper::toResponse)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  @Transactional(readOnly = true)
  public UserAccount entityByEmail(String email) {
    return repository
        .findByEmail(EmailNormalizer.normalize(email))
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  public void requestPasswordReset(String email) {
    repository
        .findByEmail(EmailNormalizer.normalize(email))
        .filter(UserAccount::isAtivo)
        .ifPresent(firstAccessService::issuePasswordReset);
  }
}
