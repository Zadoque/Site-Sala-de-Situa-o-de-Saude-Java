package com.example.demo.service;

import com.example.demo.DTO.request.FirstAccessRequest;
import com.example.demo.entity.FirstAccessToken;
import com.example.demo.entity.User;
import com.example.demo.repository.FirstAccessTokenRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
public class FirstAccessService {
    private final UserRepository users;
    private final FirstAccessTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailService mail;
    @Value("${api.first-access.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${api.first-access.expiration:86400}") private long expirationSeconds;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void issue(User user) {
        tokens.findByUserId(user.getId()).ifPresent(tokens::delete);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        FirstAccessToken token = new FirstAccessToken();
        token.setUser(user); token.setTokenHash(hash(raw)); token.setStatus("PENDING"); token.setPurpose("FIRST_ACCESS");
        token.setSentAt(Instant.now()); token.setExpiresAt(Instant.now().plus(Duration.ofSeconds(expirationSeconds)));
        tokens.save(token);
        String link = frontendUrl.replaceAll("/$", "") + "/primeiro-acesso?token=" + raw;
        mail.sendText(user.getEmail(), "Convite de primeiro acesso ao NSS",
                "Acesse o NSS pelo link abaixo para criar seu nome e senha:\n\n" + link +
                        "\n\nEste convite expira em 24 horas e só pode ser usado uma vez.");
    }

    @Transactional
    public void issuePasswordReset(User user) {
        tokens.findByUserId(user.getId()).ifPresent(tokens::delete);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        FirstAccessToken token = new FirstAccessToken(); token.setUser(user); token.setTokenHash(hash(raw)); token.setStatus("PENDING"); token.setPurpose("PASSWORD_RESET"); token.setSentAt(Instant.now()); token.setExpiresAt(Instant.now().plus(Duration.ofSeconds(expirationSeconds))); tokens.save(token);
        String link = frontendUrl.replaceAll("/$", "") + "/redefinir-senha?token=" + raw;
        mail.sendText(user.getEmail(), "Redefinição de senha do NSS", "Use o link abaixo para criar uma nova senha:\n\n" + link + "\n\nEste link expira em 7 dias e só pode ser usado uma vez.");
    }

    @Transactional
    public void completePasswordReset(com.example.demo.DTO.request.PasswordResetCompleteRequest request) {
        FirstAccessToken invite = tokens.findByTokenHash(hash(request.token())).filter(t -> "PENDING".equals(t.getStatus()) && "PASSWORD_RESET".equals(t.getPurpose()) && t.getExpiresAt().isAfter(Instant.now())).orElseThrow(() -> new IllegalArgumentException("Link inválido ou expirado"));
        User user = invite.getUser(); if (!user.getEmail().equalsIgnoreCase(request.email())) throw new IllegalArgumentException("E-mail não corresponde ao link");
        user.setPassword(encoder.encode(request.password())); user.setPasswordCreatedAt(Instant.now()); user.setAtivo(true); users.save(user); invite.setStatus("COMPLETED"); invite.setCompletedAt(Instant.now()); invite.setPasswordCreatedAt(Instant.now()); tokens.save(invite);
    }

    @Transactional
    public void complete(FirstAccessRequest request) {
        FirstAccessToken invite = tokens.findByTokenHash(hash(request.token()))
                .filter(t -> "PENDING".equals(t.getStatus()) && "FIRST_ACCESS".equals(t.getPurpose()) && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Convite inválido ou expirado"));
        User user = invite.getUser();
        if (!user.getEmail().equalsIgnoreCase(request.email())) throw new IllegalArgumentException("E-mail não corresponde ao convite");
        user.setNome(request.name().trim()); user.setPassword(encoder.encode(request.password()));
        user.setPasswordCreatedAt(Instant.now()); user.setAtivo(true); users.save(user);
        invite.setStatus("COMPLETED"); invite.setCompletedAt(Instant.now()); invite.setPasswordCreatedAt(Instant.now()); tokens.save(invite);
    }

    public boolean pending(User user) { return tokens.findByUserId(user.getId()).map(t -> "PENDING".equals(t.getStatus()) && t.getExpiresAt().isAfter(Instant.now())).orElse(false); }
    @Transactional(readOnly = true)
    public java.util.List<com.example.demo.DTO.response.InvitationResponse> pendingInvitations() {
        return tokens.findByStatusOrderByExpiresAtAsc("PENDING").stream().map(this::view).toList();
    }
    @Transactional public com.example.demo.DTO.response.InvitationResponse renew(Long id) {
        FirstAccessToken old = tokens.findById(id).orElseThrow(() -> new IllegalArgumentException("Convite não encontrado"));
        if (!"PENDING".equals(old.getStatus())) throw new IllegalArgumentException("Convite não está pendente");
        old.setStatus("CANCELLED"); tokens.save(old); issue(old.getUser());
        return tokens.findByUserId(old.getUser().getId()).map(this::view).orElseThrow();
    }
    @Transactional public void cancel(Long id) {
        FirstAccessToken token = tokens.findById(id).orElseThrow(() -> new IllegalArgumentException("Convite não encontrado"));
        if ("PENDING".equals(token.getStatus())) { token.setStatus("CANCELLED"); tokens.save(token); }
    }
    private com.example.demo.DTO.response.InvitationResponse view(FirstAccessToken t) { return new com.example.demo.DTO.response.InvitationResponse(t.getId(), t.getUser().getId(), t.getUser().getEmail(), t.getUser().getAccountType(), t.getStatus(), t.getSentAt(), t.getExpiresAt()); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
