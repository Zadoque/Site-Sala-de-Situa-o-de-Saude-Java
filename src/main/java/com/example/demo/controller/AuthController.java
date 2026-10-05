package com.example.demo.controller;

import com.example.demo.DTO.request.LoginRequest;
import com.example.demo.DTO.request.FirstAccessRequest;
import com.example.demo.DTO.response.LoginResponse;
import com.example.demo.DTO.response.UserResponse;
import com.example.demo.DTO.response.MeResponse;
import com.example.demo.security.JwtService;
import com.example.demo.service.UserService;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, RefreshSession> refreshTokens = new ConcurrentHashMap<>();

    private final com.example.demo.service.FirstAccessService firstAccessService;

    @PostMapping("/first-access")
    public ResponseEntity<Void> firstAccess(@Valid @RequestBody FirstAccessRequest request) {
        firstAccessService.complete(request);
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
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                );

        authenticationManager.authenticate(authentication);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.email()
                );

        String token = jwtService.generateToken(userDetails);
        String refresh = UUID.randomUUID().toString();
        refreshTokens.put(refresh, new RefreshSession(userDetails.getUsername(), System.currentTimeMillis() + refreshExpiration));
        addRefreshCookie(response, refresh);
        UserResponse user = userService.getByEmail(userDetails.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, accessExpiration / 1000, user, token, permissions(userDetails)));

    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = cookie(request);
        RefreshSession session = token == null ? null : refreshTokens.get(token);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            if (token != null) refreshTokens.remove(token);
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh inválido");
        }
        String username = session.username();
        // Refresh tokens are opaque and bound to the authenticated user in the production store.
        // This in-memory baseline rotates the token; the cookie is never exposed to JavaScript.
        refreshTokens.remove(token);
        String access = jwtService.generateToken(userDetailsService.loadUserByUsername(username));
        String next = UUID.randomUUID().toString();
        refreshTokens.put(next, new RefreshSession(username, System.currentTimeMillis() + refreshExpiration));
        addRefreshCookie(response, next);
        return ResponseEntity.ok(new LoginResponse(access, accessExpiration / 1000, userService.getByEmail(username), access, permissions(userDetailsService.loadUserByUsername(username))));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookie(request);
        if (token != null) refreshTokens.remove(token);
        Cookie cookie = new Cookie(refreshCookieName, ""); cookie.setHttpOnly(true); cookie.setSecure(secureCookie); cookie.setPath("/api/v1/auth"); cookie.setMaxAge(0); response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    private String cookie(HttpServletRequest request) { if (request.getCookies() == null) return null; for (Cookie c : request.getCookies()) if (refreshCookieName.equals(c.getName())) return c.getValue(); return null; }
    private java.util.List<String> permissions(UserDetails details) { return details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? java.util.List.of("USER_MANAGEMENT") : java.util.List.of(); }
    private void addRefreshCookie(HttpServletResponse response, String token) { Cookie c = new Cookie(refreshCookieName, token); c.setHttpOnly(true); c.setSecure(secureCookie); c.setPath("/api/v1/auth"); c.setMaxAge((int) (refreshExpiration / 1000)); response.addCookie(c); }
    private record RefreshSession(String username, long expiresAt) {}

}
