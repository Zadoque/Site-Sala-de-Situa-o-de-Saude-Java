package com.example.nss.user.entity;

public record FirstAccessCommand(String email, String name, String password, String token) {}
