package com.example.nss.user.dto.response;

import java.util.List;

public record MeResponse(String email, String name, boolean active, List<String> permissions) {}
