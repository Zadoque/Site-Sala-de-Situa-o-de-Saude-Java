package com.example.nss.DTO.response;

import com.example.nss.entity.AccountType;
import java.time.Instant;

public record ManagedUserResponse(
    Long id,
    String email,
    String name,
    AccountType accountType,
    boolean active,
    boolean firstAccessPending,
    Instant passwordCreatedAt) {}
