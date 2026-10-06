package com.example.demo.controller;

import com.example.demo.DTO.request.LoginRequest;
import com.example.demo.DTO.request.FirstAccessRequest;
import com.example.demo.DTO.request.PasswordResetRequest;
import com.example.demo.DTO.request.PasswordResetCompleteRequest;
import com.example.demo.DTO.response.LoginResponse;
import com.example.demo.DTO.response.UserResponse;
import com.example.demo.DTO.response.MeResponse;
import com.example.demo.security.JwtService;
import com.example.demo.service.UserService;
import com.example.demo.service.AuthRateLimiter;
import com.example.demo.service.RefreshSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;

import java.time.Duration;

@RestController
@RequestMapping({"/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    @Value("${jwt.expiration:3600000}") private long accessExpiration;
    @Value("${api.auth.refresh-cookie-name:nss_refresh}") private String refreshCookieName;
    @Value("${api.auth.refresh-expiration:604800000}") private long refreshExpiration;
    @Value("${api.auth.secure-cookie:false}") private boolean secureCookie;
    private final com.example.demo.service.FirstAccessService firstAccessService;
    private final RefreshSessionService refreshSessions;
    private final AuthRateLimiter rateLimiter;

    @PostMapping("/first-access")
    public ResponseEntity<Void> firstAccess(@Valid @RequestBody FirstAccessRequest request) {
        firstAccessService.complete(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        rateLimiter.registerPasswordResetRequest(request.email());
        userService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        firstAccessService.completePasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        UserResponse user = userService.getByEmail(authentication.getName());
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(new MeResponse(user.email(), user.nome(), user.ativo(), admin ? java.util.List.of("USER_MANAGEMENT") : java.util.List.of()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        rateLimiter.checkLogin(request.email());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                );

        try {
            authenticationManager.authenticate(authentication);
        } catch (AuthenticationException exception) {
            rateLimiter.loginFailed(request.email());
            throw exception;
        }
        rateLimiter.loginSucceeded(request.email());

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.email()
                );

        String token = jwtService.generateToken(userDetails);
        String refresh = refreshSessions.issue(userService.entityByEmail(userDetails.getUsername()));
        addRefreshCookie(response, refresh);
        UserResponse user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, accessExpiration / 1000, user, token, permissions(userDetails)));

    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = cookie(request);
        RefreshSessionService.Rotation rotation = refreshSessions.rotateWithToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(rotation.user().getEmail());
        String access = jwtService.generateToken(userDetails);
        addRefreshCookie(response, rotation.rawToken());
        return ResponseEntity.ok(new LoginResponse(access, accessExpiration / 1000, userService.getByEmail(userDetails.getUsername()), access, permissions(userDetails)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookie(request);
        refreshSessions.revoke(token);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String cookie(HttpServletRequest request) { if (request.getCookies() == null) return null; for (jakarta.servlet.http.Cookie c : request.getCookies()) if (refreshCookieName.equals(c.getName())) return c.getValue(); return null; }
    private java.util.List<String> permissions(UserDetails details) { return details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? java.util.List.of("USER_MANAGEMENT") : java.util.List.of(); }
    private void addRefreshCookie(HttpServletResponse response, String token) { response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(refreshCookieName, token).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ofMillis(refreshExpiration)).build().toString()); }
    private void clearRefreshCookie(HttpServletResponse response) { response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(refreshCookieName, "").httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ZERO).build().toString()); }

}
