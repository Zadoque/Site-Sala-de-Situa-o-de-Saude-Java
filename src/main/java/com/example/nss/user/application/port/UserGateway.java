package com.example.nss.user.application.port;

import com.example.nss.user.domain.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserGateway {
  Optional<UserAccount> findByEmail(String email);

  Optional<UserAccount> findById(Long id);

  UserAccount save(UserAccount user);

  Page search(String search, int page, int size);

  record Page(List<UserAccount> items, int page, int size, boolean hasNext, long totalItems) {}
}
