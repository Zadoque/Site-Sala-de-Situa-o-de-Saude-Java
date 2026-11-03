package com.example.nss.user.domain;

import java.time.Instant;

public class UserAccount {
  private Long id;
  private String nome;
  private String email;
  private String password;
  private AccountType accountType = AccountType.USER;
  private boolean ativo = true;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  private Instant passwordCreatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public void setAccountType(AccountType accountType) {
    this.accountType = accountType;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    this.createdAt = value;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    this.updatedAt = value;
  }

  public Instant getPasswordCreatedAt() {
    return passwordCreatedAt;
  }

  public void setPasswordCreatedAt(Instant value) {
    this.passwordCreatedAt = value;
  }
}
