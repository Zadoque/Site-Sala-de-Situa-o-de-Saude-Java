package com.example.demo.service;

import com.example.demo.DTO.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final FirstAccessService firstAccessService;
    public UserResponse getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(this::response)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }
    public UserResponse response(User user) { return new UserResponse(user.getId(), user.getNome(), user.getEmail(), user.isAtivo()); }
    public void requestPasswordReset(String email) { repository.findByEmailIgnoreCase(email).filter(User::isAtivo).ifPresent(firstAccessService::issuePasswordReset); }
}
