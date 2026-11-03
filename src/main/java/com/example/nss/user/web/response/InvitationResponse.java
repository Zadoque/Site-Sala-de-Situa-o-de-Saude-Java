package com.example.nss.user.web.response;

import com.example.nss.user.domain.AccountType;
import java.time.Instant;

public record InvitationResponse(
    Long id,
    Long userId,
    String email,
    AccountType accountType,
    String status,
    Instant sentAt,
    Instant expiresAt)
    implements com.example.nss.user.application.query.InvitationView {}
