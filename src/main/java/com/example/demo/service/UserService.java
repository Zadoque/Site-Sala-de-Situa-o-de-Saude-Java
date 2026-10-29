package com.example.demo.service;

import com.example.demo.DTO.mapper.UserMapper;
import com.example.demo.DTO.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final FirstAccessService firstAccessService;
    private final UserMapper mapper;

    public UserResponse getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(mapper::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public User entityByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }
    public void requestPasswordReset(String email) { repository.findByEmailIgnoreCase(email).filter(User::isAtivo).ifPresent(firstAccessService::issuePasswordReset); }
}
