package com.example.architecture.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class IllegalDomain {
  @Id private Long id;
}
