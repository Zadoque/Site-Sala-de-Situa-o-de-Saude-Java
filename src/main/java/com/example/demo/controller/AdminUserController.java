package com.example.demo.controller;

import com.example.demo.DTO.request.AccountInvitationRequest;
import com.example.demo.DTO.request.ActivationRequest;
import com.example.demo.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService service;
    @GetMapping public Object search(@RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "60") int size) { if (page < 0 || size < 1 || size > 60) throw new IllegalArgumentException("page deve ser >= 0 e size deve estar entre 1 e 60"); return service.search(search, page, size); }
    @PostMapping public Object invite(@Valid @RequestBody AccountInvitationRequest request) { return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(service.invite(request)); }
    @PatchMapping("/{id}/activation") public Object activation(@PathVariable Long id, @Valid @RequestBody ActivationRequest request, Authentication auth) { return service.setActive(id, request.active(), auth.getName()); }
    @PostMapping("/{id}/password-reset") @ResponseStatus(HttpStatus.NO_CONTENT) public void reset(@PathVariable Long id) { service.resetPassword(id); }
    @GetMapping("/invitations") public Object invitations() { return service.invitations(); }
    @PostMapping("/invitations/{id}/renew") public Object renew(@PathVariable Long id) { return service.renewInvitation(id); }
    @PostMapping("/invitations/{id}/cancel") @ResponseStatus(HttpStatus.NO_CONTENT) public void cancel(@PathVariable Long id) { service.cancelInvitation(id); }
}
