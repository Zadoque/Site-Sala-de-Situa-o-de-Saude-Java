package com.example.nss.user.web.response;

public record UserResponse(Long id, String nome, String email, boolean ativo)
    implements com.example.nss.user.application.query.UserView {}
