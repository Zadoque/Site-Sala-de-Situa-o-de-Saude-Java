package com.example.nss.user.web.controller;

import com.example.nss.user.application.AdminUserService;
import com.example.nss.user.application.query.InvitationView;
import com.example.nss.user.application.query.ManagedUserView;
import com.example.nss.user.application.query.UserPageView;
import com.example.nss.user.web.request.AccountInvitationRequest;
import com.example.nss.user.web.request.ActivationRequest;
import com.example.nss.user.web.response.InvitationResponse;
import com.example.nss.user.web.response.ManagedUserResponse;
import com.example.nss.user.web.response.UserPageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
  private final AdminUserService service;

  @GetMapping
  public UserPageResponse search(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "60") int size) {
    if (page < 0 || size < 1 || size > 60) {
      throw new IllegalArgumentException("page deve ser >= 0 e size deve estar entre 1 e 60");
    }
    UserPageView result = service.<UserPageView>search(search, page, size);
    return new UserPageResponse(
        result.items().stream().map(this::toResponse).toList(),
        result.page(),
        result.size(),
        result.hasNext(),
        result.totalItems());
  }

  @PostMapping
  public ResponseEntity<ManagedUserResponse> invite(
      @Valid @RequestBody AccountInvitationRequest request) {
    ManagedUserView result = service.<ManagedUserView>invite(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
  }

  @PatchMapping("/{id}/activation")
  public ManagedUserResponse activation(
      @PathVariable Long id, @Valid @RequestBody ActivationRequest request, Authentication auth) {
    return toResponse(service.<ManagedUserView>setActive(id, request.active(), auth.getName()));
  }

  @PostMapping("/{id}/password-reset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reset(@PathVariable Long id) {
    service.resetPassword(id);
  }

  @GetMapping("/invitations")
  public List<InvitationResponse> invitations() {
    return service.invitations().stream().map(this::toResponse).toList();
  }

  @PostMapping("/invitations/{id}/renew")
  public InvitationResponse renew(@PathVariable Long id) {
    return toResponse(service.<InvitationView>renewInvitation(id));
  }

  private InvitationResponse toResponse(InvitationView invitation) {
    return new InvitationResponse(
        invitation.id(),
        invitation.userId(),
        invitation.email(),
        invitation.accountType(),
        invitation.status(),
        invitation.sentAt(),
        invitation.expiresAt());
  }

  private ManagedUserResponse toResponse(ManagedUserView user) {
    return new ManagedUserResponse(
        user.id(),
        user.email(),
        user.name(),
        user.accountType(),
        user.active(),
        user.firstAccessPending(),
        user.passwordCreatedAt());
  }

  @PostMapping("/invitations/{id}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long id) {
    service.cancelInvitation(id);
  }
}
