package com.example.nss.shared.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.nss.shared.exception.MailDeliveryException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class MailServiceTest {
  @Test
  void sendsConfiguredTextMessage() {
    JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
    MailService service = new MailService(sender);
    ReflectionTestUtils.setField(service, "from", "nss@example.com");

    service.sendText("user@example.com", "Subject", "Body");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(captor.capture());
    org.junit.jupiter.api.Assertions.assertTrue(matches(captor.getValue()));
  }

  @Test
  void omitsBlankSenderAddress() {
    JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
    MailService service = new MailService(sender);
    ReflectionTestUtils.setField(service, "from", " ");

    service.sendText("user@example.com", "Subject", "Body");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(captor.capture());
    org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getFrom());
  }

  @Test
  void sendsOnlyAfterTheCurrentTransactionCommits() {
    JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
    MailService service = new MailService(sender);
    TransactionSynchronizationManager.initSynchronization();

    try {
      service.sendTextAfterCommit("user@example.com", "Subject", "Body");
      org.mockito.Mockito.verifyNoInteractions(sender);

      TransactionSynchronizationUtils.invokeAfterCommit(
          TransactionSynchronizationManager.getSynchronizations());

      verify(sender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void reportsRequiredDeliveryFailureWithoutExposingTransportDetails() {
    JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
    MailService service = new MailService(sender);
    doThrow(new RuntimeException("smtp credential details"))
        .when(sender)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

    MailDeliveryException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            MailDeliveryException.class,
            () -> service.sendTextRequired("user@example.com", "Subject", "Body"));

    org.junit.jupiter.api.Assertions.assertEquals(
        "Não foi possível enviar o e-mail. Verifique a configuração SMTP.", exception.getMessage());
  }

  private static boolean matches(SimpleMailMessage message) {
    return message.getFrom().equals("nss@example.com")
        && message.getTo()[0].equals("user@example.com")
        && message.getSubject().equals("Subject")
        && message.getText().equals("Body");
  }
}
