package com.example.nss.user.entity;

import java.time.Instant;

public record ManagedUserView(
    Long id,
    String email,
    String name,
    AccountType accountType,
    boolean active,
    boolean firstAccessPending,
    Instant passwordCreatedAt) {}
