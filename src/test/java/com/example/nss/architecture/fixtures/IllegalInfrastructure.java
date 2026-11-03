package com.example.architecture.fixtures;

import com.example.nss.analytics.web.controller.AnalyticsController;

public class IllegalInfrastructure {
  private final AnalyticsController controller;

  public IllegalInfrastructure(AnalyticsController controller) {
    this.controller = controller;
  }
}
