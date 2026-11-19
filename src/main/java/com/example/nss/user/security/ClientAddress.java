package com.example.nss.user.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ClientAddress {
  private ClientAddress() {}

  public static String current() {
    // Tomcat normalizes RemoteAddr only for the explicitly configured trusted proxies.
    return RequestContextHolder.getRequestAttributes()
            instanceof ServletRequestAttributes attributes
        ? attributes.getRequest().getRemoteAddr()
        : "unknown";
  }
}
