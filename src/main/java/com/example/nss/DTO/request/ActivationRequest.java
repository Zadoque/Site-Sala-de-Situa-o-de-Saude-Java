package com.example.nss.DTO.request;

import jakarta.validation.constraints.NotNull;

public record ActivationRequest(@NotNull Boolean active) {}
