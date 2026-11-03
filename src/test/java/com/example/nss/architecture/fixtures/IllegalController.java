package com.example.architecture.fixtures;

import com.example.nss.analytics.infrastructure.repository.AnalyticsRepository;

public class IllegalController {
  private final AnalyticsRepository repository;

  public IllegalController(AnalyticsRepository repository) {
    this.repository = repository;
  }
}
