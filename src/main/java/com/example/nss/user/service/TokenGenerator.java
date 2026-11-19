package com.example.nss.user.service;

@FunctionalInterface
public interface TokenGenerator {
  void nextBytes(byte[] bytes);
}
