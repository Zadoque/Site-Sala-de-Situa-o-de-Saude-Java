package com.example.nss.user.dto.request;

import com.example.nss.user.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AccountInvitationRequest(
    @Email @jakarta.validation.constraints.NotBlank String email,
    @NotNull AccountType accountType) {}
