package com.example.demo.service;

import com.example.demo.DTO.request.AccountInvitationRequest;
import com.example.demo.DTO.response.ManagedUserResponse;
import com.example.demo.entity.AccountType;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository users;
    private final FirstAccessService firstAccess;

    public List<ManagedUserResponse> search(String search) {
        List<User> result = search == null || search.isBlank() ? users.findAll() : users.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search);
        return result.stream().map(this::view).toList();
    }

    @Transactional
    public ManagedUserResponse invite(AccountInvitationRequest request) {
        if (users.findByEmailIgnoreCase(request.email()).isPresent()) throw new IllegalArgumentException("E-mail já cadastrado");
        User user = new User(); user.setEmail(request.email().trim().toLowerCase()); user.setAccountType(request.accountType()); user.setAtivo(true); user.setCreatedAt(Instant.now()); user.setUpdatedAt(Instant.now());
        User saved = users.save(user); firstAccess.issue(saved); return view(saved);
    }

    @Transactional
    public ManagedUserResponse setActive(Long id, boolean active, String actorEmail) {
        User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        if (user.getEmail().equalsIgnoreCase(actorEmail) && !active) throw new IllegalArgumentException("Você não pode desativar sua própria conta");
        if (!active && user.getAccountType() == AccountType.ADMIN) throw new IllegalArgumentException("Admin não pode desativar outro admin");
        user.setAtivo(active); return view(users.save(user));
    }

    @Transactional public void resetPassword(Long id) { User user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado")); user.setPassword(null); user.setPasswordCreatedAt(null); users.save(user); firstAccess.issue(user); }
    public java.util.List<com.example.demo.DTO.response.InvitationResponse> invitations() { return firstAccess.pendingInvitations(); }
    public com.example.demo.DTO.response.InvitationResponse renewInvitation(Long id) { return firstAccess.renew(id); }
    public void cancelInvitation(Long id) { firstAccess.cancel(id); }
    private ManagedUserResponse view(User u) { return new ManagedUserResponse(u.getId(), u.getEmail(), u.getNome(), u.getAccountType(), u.isAtivo(), firstAccess.pending(u), u.getPasswordCreatedAt()); }
}
