package com.example.nss.user.application.command;

public interface PasswordResetCommand {
  String email();

  String token();

  String password();
}
