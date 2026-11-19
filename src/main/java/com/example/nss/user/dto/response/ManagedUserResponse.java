package com.example.nss.user.dto.response;

import com.example.nss.user.entity.AccountType;
import java.time.Instant;

public record ManagedUserResponse(
    Long id,
    String email,
    String name,
    AccountType accountType,
    boolean active,
    boolean firstAccessPending,
    Instant passwordCreatedAt) {}
