package com.example.nss.user.application.query;

import java.util.List;

public record AuthenticationResult(
    String accessToken,
    String refreshToken,
    UserView user,
    long accessExpirationSeconds,
    List<String> permissions) {}
