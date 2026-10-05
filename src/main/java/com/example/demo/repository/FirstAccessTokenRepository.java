package com.example.demo.repository;

import com.example.demo.entity.FirstAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FirstAccessTokenRepository extends JpaRepository<FirstAccessToken, Long> {
    Optional<FirstAccessToken> findByTokenHash(String tokenHash);
    Optional<FirstAccessToken> findByUserId(Long userId);
    java.util.List<FirstAccessToken> findByStatusOrderByExpiresAtAsc(String status);
}
