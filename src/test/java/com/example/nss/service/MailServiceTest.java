package com.example.nss.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

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

  private static boolean matches(SimpleMailMessage message) {
    return message.getFrom().equals("nss@example.com")
        && message.getTo()[0].equals("user@example.com")
        && message.getSubject().equals("Subject")
        && message.getText().equals("Body");
  }
}
