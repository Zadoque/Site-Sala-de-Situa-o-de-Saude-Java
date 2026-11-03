package com.example.nss.user.web.controller;

import com.example.nss.user.application.AuthenticationService;
import com.example.nss.user.application.query.AuthenticationResult;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.web.request.FirstAccessRequest;
import com.example.nss.user.web.request.LoginRequest;
import com.example.nss.user.web.request.PasswordResetCompleteRequest;
import com.example.nss.user.web.request.PasswordResetRequest;
import com.example.nss.user.web.response.LoginResponse;
import com.example.nss.user.web.response.MeResponse;
import com.example.nss.user.web.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationService authenticationService;

  @Value("${api.auth.refresh-cookie-name:nss_refresh}")
  private String refreshCookieName;

  @Value("${api.auth.refresh-expiration:604800000}")
  private long refreshExpiration;

  @Value("${api.auth.secure-cookie:false}")
  private boolean secureCookie;

  private final CookieCsrfTokenRepository csrfTokenRepository;

  @PostMapping("/first-access")
  public ResponseEntity<Void> firstAccess(@Valid @RequestBody FirstAccessRequest request) {
    authenticationService.completeFirstAccess(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password-reset/request")
  public ResponseEntity<Void> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request) {
    authenticationService.requestPasswordReset(request.email());
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/password-reset/complete")
  public ResponseEntity<Void> completePasswordReset(
      @Valid @RequestBody PasswordResetCompleteRequest request) {
    authenticationService.completePasswordReset(request);
    return ResponseEntity.noContent().build();
  }

  @org.springframework.web.bind.annotation.GetMapping("/me")
  public ResponseEntity<MeResponse> me(Authentication authentication) {
    AuthenticationService.MeResult result = authenticationService.me(authentication.getName());
    UserView user = result.user();
    return ResponseEntity.ok(
        new MeResponse(user.email(), user.nome(), user.ativo(), result.permissions()));
  }

  @org.springframework.web.bind.annotation.GetMapping("/csrf")
  public ResponseEntity<String> csrf(
      CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
    csrfTokenRepository.saveToken(token, request, response);
    return ResponseEntity.ok(token.getToken());
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthenticationResult result = authenticationService.login(request.email(), request.password());
    addRefreshCookie(response, result.refreshToken());
    return ResponseEntity.ok(
        new LoginResponse(
            result.accessToken(),
            result.accessExpirationSeconds(),
            response(result.user()),
            result.accessToken(),
            result.permissions()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    AuthenticationResult result = authenticationService.refresh(cookie(request));
    addRefreshCookie(response, result.refreshToken());
    return ResponseEntity.ok(
        new LoginResponse(
            result.accessToken(),
            result.accessExpirationSeconds(),
            response(result.user()),
            result.accessToken(),
            result.permissions()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    authenticationService.logout(cookie(request));
    clearRefreshCookie(response);
    return ResponseEntity.noContent().build();
  }

  private String cookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (jakarta.servlet.http.Cookie c : request.getCookies())
      if (refreshCookieName.equals(c.getName())) return c.getValue();
    return null;
  }

  private UserResponse response(UserView user) {
    return new UserResponse(user.id(), user.nome(), user.email(), user.ativo());
  }

  private void addRefreshCookie(HttpServletResponse response, String token) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(refreshCookieName, token)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ofMillis(refreshExpiration))
            .build()
            .toString());
  }

  private void clearRefreshCookie(HttpServletResponse response) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(refreshCookieName, "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(Duration.ZERO)
            .build()
            .toString());
  }
}
