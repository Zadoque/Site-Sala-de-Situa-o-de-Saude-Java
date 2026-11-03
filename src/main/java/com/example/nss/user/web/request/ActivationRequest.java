package com.example.nss.user.web.request;

import jakarta.validation.constraints.NotNull;

public record ActivationRequest(@NotNull Boolean active) {}
