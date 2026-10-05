package com.example.demo.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(@Email @NotBlank String email, @NotBlank String token,
                                           @NotBlank @Size(min = 12, max = 128) String password) {}
