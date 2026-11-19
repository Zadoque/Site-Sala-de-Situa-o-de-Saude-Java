package com.example.nss.user.service;

import com.example.nss.user.entity.User;
import com.example.nss.user.repository.UserRepository;
import com.example.nss.user.security.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repository;
  private final FirstAccessService firstAccessService;

  public User getByEmail(String email) {
    return repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  @Transactional(readOnly = true)
  public User entityByEmail(String email) {
    return repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
  }

  @Transactional(readOnly = true)
  public User entityById(long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new org.springframework.security.authentication.BadCredentialsException(
                    "Conta inválida"));
  }

  public void requestPasswordReset(String email) {
    repository
        .findByEmailIgnoreCase(EmailNormalizer.normalize(email))
        .filter(User::isAtivo)
        .ifPresent(firstAccessService::issuePasswordReset);
  }
}
