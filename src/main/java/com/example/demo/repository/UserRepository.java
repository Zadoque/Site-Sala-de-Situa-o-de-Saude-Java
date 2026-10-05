package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    java.util.List<User> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(String nome, String email);

    Page<User> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(String nome, String email, Pageable pageable);

    boolean existsByEmail(String email);

    long countByAccountTypeAndAtivoTrue(com.example.demo.entity.AccountType accountType);
}
