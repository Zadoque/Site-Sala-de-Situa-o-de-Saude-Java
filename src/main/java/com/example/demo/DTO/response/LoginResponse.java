package com.example.demo.DTO.response;

public record LoginResponse(String accessToken, long expiresIn, UserResponse user, String token, java.util.List<String> permissions) {
    public LoginResponse(String accessToken, long expiresIn, UserResponse user) {
        this(accessToken, expiresIn, user, accessToken, java.util.List.of());
    }
}
