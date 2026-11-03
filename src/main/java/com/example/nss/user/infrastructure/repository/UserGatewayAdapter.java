package com.example.nss.user.infrastructure.repository;

import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.infrastructure.persistence.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserGatewayAdapter implements UserGateway {
  private final UserRepository repository;

  public Optional<UserAccount> findByEmail(String email) {
    return repository.findByEmailIgnoreCase(email).map(this::toDomain);
  }

  public Optional<UserAccount> findById(Long id) {
    return repository.findById(id).map(this::toDomain);
  }

  public UserAccount save(UserAccount user) {
    return toDomain(repository.save(toEntity(user)));
  }

  public Page search(String search, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by("email").ascending());
    var result =
        search == null || search.isBlank()
            ? repository.findAll(pageable)
            : repository.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, pageable);
    return new Page(
        result.getContent().stream().map(this::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.hasNext(),
        result.getTotalElements());
  }

  private UserAccount toDomain(User entity) {
    UserAccount user = new UserAccount();
    user.setId(entity.getId());
    user.setNome(entity.getNome());
    user.setEmail(entity.getEmail());
    user.setPassword(entity.getPassword());
    user.setAccountType(entity.getAccountType());
    user.setAtivo(entity.isAtivo());
    user.setCreatedAt(entity.getCreatedAt());
    user.setUpdatedAt(entity.getUpdatedAt());
    user.setPasswordCreatedAt(entity.getPasswordCreatedAt());
    return user;
  }

  private User toEntity(UserAccount user) {
    User entity = new User();
    entity.setId(user.getId());
    entity.setNome(user.getNome());
    entity.setEmail(user.getEmail());
    entity.setPassword(user.getPassword());
    entity.setAccountType(user.getAccountType());
    entity.setAtivo(user.isAtivo());
    entity.setCreatedAt(user.getCreatedAt());
    entity.setUpdatedAt(user.getUpdatedAt());
    entity.setPasswordCreatedAt(user.getPasswordCreatedAt());
    return entity;
  }
}
