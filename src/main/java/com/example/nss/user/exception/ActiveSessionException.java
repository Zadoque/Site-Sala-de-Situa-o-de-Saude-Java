package com.example.nss.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Signals that the account already owns the single permitted active session. */
@ResponseStatus(HttpStatus.CONFLICT)
public class ActiveSessionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ActiveSessionException() {
    super("Este usuário já está conectado em outra sessão. Escolha se deseja encerrá-la.");
  }
}
