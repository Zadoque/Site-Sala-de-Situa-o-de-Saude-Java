package com.example.nss.user.application;

import com.example.nss.user.application.command.InvitationCommand;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.application.port.UserMapperPort;
import com.example.nss.user.application.query.InvitationView;
import com.example.nss.user.application.query.ManagedUserView;
import com.example.nss.user.application.query.UserPageResult;
import com.example.nss.user.application.query.UserPageView;
import com.example.nss.user.domain.AccountType;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.domain.rule.EmailNormalizer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AdminUserService {
  private final UserGateway users;
  private final FirstAccessService firstAccess;
  private final RefreshSessionService refreshSessions;
  private final UserMapperPort mapper;

  @SuppressWarnings("unchecked")
  public <T extends UserPageView> T search(String search, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 60);
    UserGateway.Page result = users.search(search, page, safeSize);
    return (T)
        new UserPageResult(
            result.items().stream().map(this::view).toList(),
            result.page(),
            result.size(),
            result.hasNext(),
            result.totalItems());
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public <T extends ManagedUserView> T invite(InvitationCommand request) {
    String normalizedEmail = EmailNormalizer.normalize(request.email());
    if (users.findByEmail(normalizedEmail).isPresent())
      throw new IllegalArgumentException("E-mail já cadastrado");
    UserAccount user = new UserAccount();
    user.setEmail(normalizedEmail);
    user.setAccountType(request.accountType());
    user.setAtivo(true);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    UserAccount saved;
    try {
      saved = users.save(user);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("E-mail já cadastrado", exception);
    }
    firstAccess.issue(saved);
    return (T) view(saved);
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public <T extends ManagedUserView> T setActive(Long id, boolean active, String actorEmail) {
    UserAccount user =
        users
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    if (user.getEmail().equalsIgnoreCase(actorEmail) && !active)
      throw new IllegalArgumentException("Você não pode desativar sua própria conta");
    if (!active && user.getAccountType() == AccountType.ADMIN)
      throw new IllegalArgumentException("Admin não pode desativar outro admin");
    user.setAtivo(active);
    UserAccount saved = users.save(user);
    if (!active) refreshSessions.revokeAllForUser(saved.getId());
    return (T) view(saved);
  }

  @Transactional
  public void resetPassword(Long id) {
    UserAccount user =
        users
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    user.setPassword(null);
    user.setPasswordCreatedAt(null);
    users.save(user);
    refreshSessions.revokeAllForUser(user.getId());
    firstAccess.issuePasswordReset(user);
  }

  @SuppressWarnings("unchecked")
  public <T extends InvitationView> java.util.List<T> invitations() {
    return (java.util.List<T>) firstAccess.pendingInvitations();
  }

  @SuppressWarnings("unchecked")
  public <T extends InvitationView> T renewInvitation(Long id) {
    return (T) firstAccess.renew(id);
  }

  public void cancelInvitation(Long id) {
    firstAccess.cancel(id);
  }

  private ManagedUserView view(com.example.nss.user.domain.UserAccount user) {
    return mapper.toManagedResponse(user, firstAccess.pending(user));
  }
}
