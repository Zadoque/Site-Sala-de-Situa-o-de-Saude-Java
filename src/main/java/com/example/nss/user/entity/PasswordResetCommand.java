package com.example.nss.user.entity;

public record PasswordResetCommand(String email, String password, String token) {}
