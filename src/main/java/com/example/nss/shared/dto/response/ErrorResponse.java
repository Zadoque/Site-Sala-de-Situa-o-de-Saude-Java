package com.example.nss.shared.dto.response;

import java.time.Instant;

public record ErrorResponse(
    Instant timestamp, int status, String code, String message, String path, String requestId) {}
