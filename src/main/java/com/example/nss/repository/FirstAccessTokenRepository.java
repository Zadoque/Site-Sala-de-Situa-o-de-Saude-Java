package com.example.nss.repository;

import com.example.nss.entity.FirstAccessToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirstAccessTokenRepository extends JpaRepository<FirstAccessToken, Long> {
  Optional<FirstAccessToken> findByTokenHash(String tokenHash);

  Optional<FirstAccessToken> findByUserId(Long userId);

  java.util.List<FirstAccessToken> findByStatusOrderByExpiresAtAsc(String status);
}
