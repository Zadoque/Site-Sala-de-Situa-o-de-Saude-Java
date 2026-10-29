package com.example.nss.DTO.response;

import com.example.nss.entity.AccountType;
import java.time.Instant;

public record InvitationResponse(
    Long id,
    Long userId,
    String email,
    AccountType accountType,
    String status,
    Instant sentAt,
    Instant expiresAt) {}
