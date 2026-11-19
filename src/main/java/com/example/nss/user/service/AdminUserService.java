package com.example.nss.user.service;

import com.example.nss.user.entity.AccountInvitationCommand;
import com.example.nss.user.entity.AccountType;
import com.example.nss.user.entity.InvitationView;
import com.example.nss.user.entity.ManagedUserView;
import com.example.nss.user.entity.User;
import com.example.nss.user.entity.UserPageView;
import com.example.nss.user.repository.UserRepository;
import com.example.nss.user.security.EmailNormalizer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {
  private final UserRepository users;
  private final FirstAccessService firstAccess;
  private final RefreshSessionService refreshSessions;

  public UserPageView search(String search, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 60);
    org.springframework.data.domain.Page<User> result =
        search == null || search.isBlank()
            ? users.findAll(
                org.springframework.data.domain.PageRequest.of(
                    page, safeSize, org.springframework.data.domain.Sort.by("email").ascending()))
            : users.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search,
                search,
                org.springframework.data.domain.PageRequest.of(
                    page, safeSize, org.springframework.data.domain.Sort.by("email").ascending()));
    return new UserPageView(
        result.getContent().stream().map(this::view).toList(),
        result.getNumber(),
        result.getSize(),
        result.hasNext(),
        result.getTotalElements());
  }

  @Transactional
  public ManagedUserView invite(AccountInvitationCommand request) {
    String normalizedEmail = EmailNormalizer.normalize(request.email());
    if (users.findByEmailIgnoreCase(normalizedEmail).isPresent())
      throw new IllegalArgumentException("E-mail já cadastrado");
    User user = new User();
    user.setEmail(normalizedEmail);
    user.setAccountType(request.accountType());
    user.setAtivo(true);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    User saved;
    try {
      saved = users.save(user);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("E-mail já cadastrado", exception);
    }
    firstAccess.issueForAdmin(saved);
    return view(saved);
  }

  @Transactional
  public ManagedUserView setActive(Long id, boolean active, String actorEmail) {
    User user =
        users
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    if (user.getEmail().equalsIgnoreCase(actorEmail) && !active)
      throw new IllegalArgumentException("Você não pode desativar sua própria conta");
    if (!active && user.getAccountType() == AccountType.ADMIN)
      throw new IllegalArgumentException("Admin não pode desativar outro admin");
    boolean wasActive = user.isAtivo();
    user.setAtivo(active);
    User saved = users.saveAndFlush(user);
    if (!active || !wasActive) refreshSessions.revokeAllForUser(saved.getId());
    return view(saved);
  }

  @Transactional
  public void resetPassword(Long id) {
    User user =
        users
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    user.setPassword(null);
    user.setPasswordCreatedAt(null);
    users.saveAndFlush(user);
    refreshSessions.revokeAllForUser(user.getId());
    firstAccess.issuePasswordResetForAdmin(user);
  }

  public java.util.List<InvitationView> invitations() {
    return firstAccess.pendingInvitations();
  }

  public InvitationView renewInvitation(Long id) {
    return firstAccess.renew(id);
  }

  public void cancelInvitation(Long id) {
    firstAccess.cancel(id);
  }

  private ManagedUserView view(User user) {
    return new ManagedUserView(
        user.getId(),
        user.getEmail(),
        user.getNome(),
        user.getAccountType(),
        user.isAtivo(),
        firstAccess.pending(user),
        user.getPasswordCreatedAt());
  }
}
