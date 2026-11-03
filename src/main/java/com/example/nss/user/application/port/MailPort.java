package com.example.nss.user.application.port;

@FunctionalInterface
public interface MailPort {
  void sendTextAfterCommit(String recipient, String subject, String body);
}
