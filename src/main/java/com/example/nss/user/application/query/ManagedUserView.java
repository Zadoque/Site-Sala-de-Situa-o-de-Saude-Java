package com.example.nss.user.application.query;

import com.example.nss.user.domain.AccountType;
import java.time.Instant;

public interface ManagedUserView {
  Long id();
  String email();
  String name();
  AccountType accountType();
  boolean active();
  boolean firstAccessPending();
  Instant passwordCreatedAt();
}
