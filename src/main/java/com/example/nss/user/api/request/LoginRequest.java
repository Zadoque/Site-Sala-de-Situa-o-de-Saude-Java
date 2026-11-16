package com.example.nss.user.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
    @NotBlank(message = "Senha é obrigatória") String password,
    Boolean replaceExistingSession) {
  public LoginRequest {
    // The field was added after the original login contract. Omitted JSON must
    // retain the safe default instead of failing deserialization.
    replaceExistingSession = Boolean.TRUE.equals(replaceExistingSession);
  }

  public LoginRequest(String email, String password) {
    this(email, password, false);
  }
}
