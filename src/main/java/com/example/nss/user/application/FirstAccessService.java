package com.example.nss.user.application;

import com.example.nss.shared.notification.MailService;
import com.example.nss.user.application.command.FirstAccessCommand;
import com.example.nss.user.application.command.PasswordResetCommand;
import com.example.nss.user.application.port.FirstAccessGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.application.query.InvitationResult;
import com.example.nss.user.application.query.InvitationView;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.security.EmailNormalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirstAccessService {
  private static final String PENDING_STATUS = "PENDING";
  private final UserGateway users;
  private final FirstAccessGateway tokens;
  private final PasswordEncoder encoder;
  private final MailService mail;
  private final RefreshSessionService refreshSessions;
  private final TokenGenerator tokenGenerator;

  @Value("${api.first-access.frontend-url:http://localhost:5173}")
  private String frontendUrl;

  @Value("${api.first-access.expiration:86400}")
  private long expirationSeconds;

  @Autowired
  public FirstAccessService(
      UserGateway users,
      FirstAccessGateway tokens,
      PasswordEncoder encoder,
      MailService mail,
      RefreshSessionService refreshSessions,
      TokenGenerator tokenGenerator) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.mail = mail;
    this.refreshSessions = refreshSessions;
    this.tokenGenerator = tokenGenerator;
  }

  public FirstAccessService(
      UserGateway users,
      FirstAccessGateway tokens,
      PasswordEncoder encoder,
      MailService mail,
      RefreshSessionService refreshSessions) {
    this(users, tokens, encoder, mail, refreshSessions, new SecureRandomTokenGenerator());
  }

  @Transactional
  public void issue(UserAccount user) {
    issueToken(
        user,
        "FIRST_ACCESS",
        "/primeiro-acesso?token=",
        "Convite de primeiro acesso ao NSS",
        "Acesse o NSS pelo link abaixo para criar seu nome e senha:");
  }

  @Transactional
  public void issuePasswordReset(UserAccount user) {
    issueToken(
        user,
        "PASSWORD_RESET",
        "/redefinir-senha?token=",
        "Redefinição de senha do NSS",
        "Use o link abaixo para criar uma nova senha:");
  }

  private void issueToken(
      UserAccount user, String purpose, String path, String subject, String intro) {
    tokens.findByUserId(user.getId()).ifPresent(this::deleteExistingToken);
    byte[] bytes = new byte[32];
    tokenGenerator.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    FirstAccessGateway.TokenData token = new FirstAccessGateway.TokenData();
    token.setUser(user);
    token.setTokenHash(hash(raw));
    token.setStatus(PENDING_STATUS);
    token.setPurpose(purpose);
    token.setSentAt(Instant.now());
    token.setExpiresAt(Instant.now().plus(Duration.ofSeconds(expirationSeconds)));
    tokens.save(token);
    String link = frontendUrl.replaceAll("/$", "") + path + raw;
    mail.sendTextAfterCommit(
        user.getEmail(),
        subject,
        intro + "\n\n" + link + "\n\nEste convite expira em 24 horas e só pode ser usado uma vez.");
  }

  @Transactional
  public void completePasswordReset(PasswordResetCommand request) {
    FirstAccessGateway.TokenData invite =
        valid(request.token(), "PASSWORD_RESET", "Link inválido ou expirado");
    UserAccount user = invite.getUser();
    if (!user.getEmail().equals(EmailNormalizer.normalize(request.email())))
      throw new IllegalArgumentException("E-mail não corresponde ao link");
    user.setPassword(encoder.encode(request.password()));
    user.setPasswordCreatedAt(Instant.now());
    users.save(user);
    refreshSessions.revokeAllForUser(user.getId());
    complete(invite);
  }

  @Transactional
  public void complete(FirstAccessCommand request) {
    FirstAccessGateway.TokenData invite =
        valid(request.token(), "FIRST_ACCESS", "Convite inválido ou expirado");
    UserAccount user = invite.getUser();
    if (!user.getEmail().equals(EmailNormalizer.normalize(request.email())))
      throw new IllegalArgumentException("E-mail não corresponde ao convite");
    user.setNome(request.name().trim());
    user.setPassword(encoder.encode(request.password()));
    user.setPasswordCreatedAt(Instant.now());
    users.save(user);
    complete(invite);
  }

  private FirstAccessGateway.TokenData valid(String raw, String purpose, String message) {
    return tokens
        .findByTokenHashForUpdate(hash(raw))
        .filter(
            token ->
                PENDING_STATUS.equals(token.getStatus())
                    && purpose.equals(token.getPurpose())
                    && token.getExpiresAt().isAfter(Instant.now()))
        .orElseThrow(() -> new IllegalArgumentException(message));
  }

  private void complete(FirstAccessGateway.TokenData token) {
    Instant now = Instant.now();
    token.setStatus("COMPLETED");
    token.setCompletedAt(now);
    token.setPasswordCreatedAt(now);
    tokens.save(token);
  }

  public boolean pending(UserAccount user) {
    return tokens
        .findByUserId(user.getId())
        .map(t -> PENDING_STATUS.equals(t.getStatus()) && t.getExpiresAt().isAfter(Instant.now()))
        .orElse(false);
  }

  @Transactional(readOnly = true)
  public List<InvitationView> pendingInvitations() {
    return tokens.findByStatusOrderByExpiresAt(PENDING_STATUS).stream().map(this::view).toList();
  }

  @Transactional
  public InvitationView renew(Long id) {
    FirstAccessGateway.TokenData old =
        tokens
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado"));
    if (!PENDING_STATUS.equals(old.getStatus()))
      throw new IllegalArgumentException("Convite não está pendente");
    old.setStatus("CANCELLED");
    tokens.save(old);
    issue(old.getUser());
    return tokens.findByUserId(old.getUser().getId()).map(this::view).orElseThrow();
  }

  @Transactional
  public void cancel(Long id) {
    FirstAccessGateway.TokenData token =
        tokens
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado"));
    if (PENDING_STATUS.equals(token.getStatus())) {
      token.setStatus("CANCELLED");
      tokens.save(token);
    }
  }

  private InvitationView view(FirstAccessGateway.TokenData token) {
    return new InvitationResult(
        token.getId(),
        token.getUser().getId(),
        token.getUser().getEmail(),
        token.getUser().getAccountType(),
        token.getStatus(),
        token.getSentAt(),
        token.getExpiresAt());
  }

  private void deleteExistingToken(FirstAccessGateway.TokenData token) {
    tokens.delete(token);
    tokens.flush();
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
