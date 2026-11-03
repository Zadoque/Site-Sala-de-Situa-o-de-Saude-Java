package com.example.nss.user.application.query;

import com.example.nss.user.domain.AccountType;
import java.time.Instant;

public interface InvitationView {
  Long id();
  Long userId();
  String email();
  AccountType accountType();
  String status();
  Instant sentAt();
  Instant expiresAt();
}
