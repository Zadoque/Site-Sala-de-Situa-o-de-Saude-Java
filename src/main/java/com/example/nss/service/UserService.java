package com.example.nss.service;

import com.example.nss.DTO.mapper.UserMapper;
import com.example.nss.DTO.response.UserResponse;
import com.example.nss.entity.User;
import com.example.nss.repository.UserRepository;
import com.example.nss.security.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repository;
  private final FirstAccessService firstAccessService;
  private final UserMapper mapper;

  public UserResponse getByEmail(String email) {
    return repository
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
