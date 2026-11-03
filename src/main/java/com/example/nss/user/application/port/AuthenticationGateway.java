package com.example.nss.user.application.port;

import com.example.nss.user.domain.UserAccount;

@FunctionalInterface
public interface AuthenticationGateway {
  AuthenticatedUser authenticate(String email, String password);

  record AuthenticatedUser(UserAccount user) {
    public String email() {
      return user.getEmail();
    }
  }
}
