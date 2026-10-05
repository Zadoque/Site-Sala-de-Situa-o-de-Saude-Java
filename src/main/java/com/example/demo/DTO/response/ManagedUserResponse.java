package com.example.demo.DTO.response;

import com.example.demo.entity.AccountType;
import java.time.Instant;

public record ManagedUserResponse(Long id, String email, String name, AccountType accountType,
                                  boolean active, boolean firstAccessPending, Instant passwordCreatedAt) {}
