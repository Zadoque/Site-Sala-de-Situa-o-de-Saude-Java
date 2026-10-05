package com.example.demo.config;

import com.example.demo.entity.AccountType;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component @RequiredArgsConstructor
public class BootstrapAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${api.bootstrap-admin.email:}") private String email;
    @Value("${api.bootstrap-admin.password:}") private String password;
    @Override public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.isBlank() || users.findByEmailIgnoreCase(email).isPresent()) return;
        User admin = new User(); admin.setEmail(email.trim().toLowerCase()); admin.setNome("Administrador NSS"); admin.setPassword(encoder.encode(password)); admin.setPasswordCreatedAt(Instant.now()); admin.setAccountType(AccountType.ADMIN); admin.setAtivo(true); admin.setCreatedAt(Instant.now()); admin.setUpdatedAt(Instant.now()); users.save(admin);
    }
}
