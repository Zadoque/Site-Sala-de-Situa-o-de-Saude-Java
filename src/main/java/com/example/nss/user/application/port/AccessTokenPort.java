package com.example.nss.user.application.port;

@FunctionalInterface
public interface AccessTokenPort {
  String generateToken(long userId);
}
