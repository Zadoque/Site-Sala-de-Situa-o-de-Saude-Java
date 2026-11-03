package com.example.architecture.fixtures;

import com.example.nss.user.application.UserService;

public class IllegalAnalytics {
  private final UserService userService;

  public IllegalAnalytics(UserService userService) {
    this.userService = userService;
  }
}
