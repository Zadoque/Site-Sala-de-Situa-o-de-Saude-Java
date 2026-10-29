package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.DTO.request.LoginRequest;
import com.example.demo.DTO.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.security.JwtService;
import com.example.demo.service.AuthRateLimiter;
import com.example.demo.service.FirstAccessService;
import com.example.demo.service.RefreshSessionService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
  @Mock UserService userService;
  @Mock AuthenticationManager authenticationManager;
  @Mock UserDetailsService userDetailsService;
  @Mock JwtService jwtService;
  @Mock FirstAccessService firstAccessService;
  @Mock RefreshSessionService refreshSessions;
  @Mock AuthRateLimiter rateLimiter;
  @Mock Authentication authentication;
  @Mock HttpServletResponse response;
  @InjectMocks AuthController controller;

  @Test
  void normalizesEmailAndReusesAuthenticatedPrincipal() {
    UserDetails principal =
        org.springframework.security.core.userdetails.User.withUsername("ana@example.com")
            .password("ignored")
            .roles("USER")
            .build();
    User user = new User();
    user.setId(7L);
    user.setEmail("ana@example.com");
    UserResponse userResponse = new UserResponse(7L, "Ana", "ana@example.com", true);

    when(authentication.getPrincipal()).thenReturn(principal);
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(jwtService.generateToken(principal)).thenReturn("access-token");
    when(userService.entityByEmail("ana@example.com")).thenReturn(user);
    when(refreshSessions.issue(user)).thenReturn("refresh-token");
    when(userService.getByEmail("ana@example.com")).thenReturn(userResponse);
    ReflectionTestUtils.setField(controller, "refreshCookieName", "nss_refresh");
    ReflectionTestUtils.setField(controller, "refreshExpiration", 604_800_000L);

    controller.login(new LoginRequest("  Ana@Example.COM ", "secret"), response);

    ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
        ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
    verify(authenticationManager).authenticate(captor.capture());
    assertEquals("ana@example.com", captor.getValue().getPrincipal());
    verify(jwtService).generateToken(principal);
    verify(userDetailsService, never()).loadUserByUsername(any());
  }
}
