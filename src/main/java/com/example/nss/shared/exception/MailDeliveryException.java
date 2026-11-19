package com.example.nss.shared.exception;

/** Indicates that a notification could not be handed to the configured SMTP server. */
public class MailDeliveryException extends RuntimeException {
  public MailDeliveryException(Throwable cause) {
    super("Não foi possível enviar o e-mail. Verifique a configuração SMTP.", cause);
  }
}
