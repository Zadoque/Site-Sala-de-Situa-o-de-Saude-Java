package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_first_access")
@Getter @Setter @NoArgsConstructor
public class FirstAccessToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "purpose", nullable = false, length = 20)
    private String purpose;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "password_created_at")
    private Instant passwordCreatedAt;
}
