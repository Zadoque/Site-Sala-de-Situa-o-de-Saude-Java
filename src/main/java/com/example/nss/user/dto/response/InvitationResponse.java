package com.example.nss.user.dto.response;

import com.example.nss.user.entity.AccountType;
import java.time.Instant;

public record InvitationResponse(
    Long id,
    Long userId,
    String email,
    AccountType accountType,
    String status,
    Instant sentAt,
    Instant expiresAt) {}
