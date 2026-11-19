package com.example.nss.user.entity;

import java.time.Instant;

public record InvitationView(
    Long id,
    Long userId,
    String email,
    AccountType accountType,
    String status,
    Instant sentAt,
    Instant expiresAt) {}
