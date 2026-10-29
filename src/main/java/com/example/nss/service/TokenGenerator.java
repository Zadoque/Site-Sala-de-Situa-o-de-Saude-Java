package com.example.nss.service;

@FunctionalInterface
public interface TokenGenerator {
  void nextBytes(byte[] bytes);
}
