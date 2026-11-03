package com.example.nss.user.web.request;

import com.example.nss.user.domain.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AccountInvitationRequest(
    @Email @jakarta.validation.constraints.NotBlank String email, @NotNull AccountType accountType)
    implements com.example.nss.user.application.command.InvitationCommand {}
