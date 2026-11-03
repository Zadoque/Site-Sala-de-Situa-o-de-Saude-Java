package com.example.nss.user.application.command;

import com.example.nss.user.domain.AccountType;

public interface InvitationCommand {
  String email();

  AccountType accountType();
}
