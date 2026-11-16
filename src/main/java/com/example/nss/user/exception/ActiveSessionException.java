package com.example.nss.user.exception;

public class ActiveSessionException extends RuntimeException {
  public ActiveSessionException() {
    super("Este usuário já está conectado em outra sessão. Escolha se deseja encerrá-la.");
  }
}
