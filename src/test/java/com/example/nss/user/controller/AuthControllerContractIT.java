package com.example.nss.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.nss.user.dto.mapper.UserMapper;
import com.example.nss.user.entity.User;
import com.example.nss.user.security.CustomUserDetailsService;
import com.example.nss.user.security.JwtService;
import com.example.nss.user.service.AuthRateLimiter;
import com.example.nss.user.service.FirstAccessService;
import com.example.nss.user.service.RefreshSessionService;
import com.example.nss.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "api.cors.allowed-origins=http://localhost:5173")
class AuthControllerContractIT {
  @Autowired MockMvc mockMvc;

  @MockitoBean UserService userService;
  @MockitoBean AuthenticationManager authenticationManager;
  @MockitoBean CustomUserDetailsService userDetailsService;
  @MockitoBean JwtService jwtService;
  @MockitoBean FirstAccessService firstAccessService;
  @MockitoBean RefreshSessionService refreshSessions;
  @MockitoBean AuthRateLimiter rateLimiter;
  @Autowired UserMapper userMapper;

  @Test
  void rejectsInvalidFirstAccessPayloadBeforeCallingTheService() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/first-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"invalid\",\"name\":\"\","
                        + "\"password\":\"short\",\"token\":\"\"}"))
        .andExpect(status().isBadRequest());

    org.mockito.Mockito.verifyNoInteractions(firstAccessService);
  }

  @Test
  void completesAValidFirstAccessRequestWithNoContent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/first-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"user@nss.test\",\"name\":\"User\","
                        + "\"password\":\"Password123!\",\"token\":\"token\"}"))
        .andExpect(status().isNoContent());

    verify(firstAccessService).complete(any());
  }

  @Test
  void acceptsPasswordResetRequestWithoutRevealingAccountExistence() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\"}"))
        .andExpect(status().isAccepted());

    verify(rateLimiter).registerPasswordResetRequest("user@nss.test");
    verify(userService).requestPasswordReset("user@nss.test");
  }

  @Test
  void exposesCsrfTokenForBrowserClients() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")));
  }

  @Test
  void refreshDoesNotRequireCsrfTokenBecauseItUsesSameSiteCookie() throws Exception {
    User user = new User();
    user.setId(1L);
    user.setEmail("user@nss.test");
    user.setPassword("encoded");
    user.setAccountVersion(0L);
    org.mockito.Mockito.when(refreshSessions.rotateWithToken("refresh-token"))
        .thenReturn(new RefreshSessionService.Rotation(user, "replacement-token"));
    org.mockito.Mockito.when(jwtService.generateToken(user)).thenReturn("access-token");

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isOk());

    verify(refreshSessions).rotateWithToken("refresh-token");
  }

  @Test
  void allowsPatchInTheConfiguredBrowserCorsContract() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                    "/api/v1/admin/users/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(
            header()
                .string(
                    "Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
  }

  @Test
  void logoutRevokesTheCookieAndExpiresItOverHttp() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(csrf())
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

    verify(refreshSessions).revoke("refresh-token");
  }

  @Test
  void loginReturnsAccessDataAndSetsTheRefreshCookie() throws Exception {
    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    User entity = new User();
    entity.setId(7L);
    entity.setEmail("user@nss.test");
    entity.setPassword("encoded");
    entity.setAccountVersion(0L);
    entity.setAccountType(com.example.nss.user.entity.AccountType.USER);
    UserDetails principal = new com.example.nss.user.security.VersionedUserDetails(entity);
    org.mockito.Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    org.mockito.Mockito.when(authenticationManager.authenticate(any())).thenReturn(authentication);
    org.mockito.Mockito.when(userService.entityById(7L)).thenReturn(entity);
    org.mockito.Mockito.when(jwtService.generateToken(entity)).thenReturn("access-token");
    org.mockito.Mockito.when(refreshSessions.issue(entity)).thenReturn("refresh-token");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.token")
                .value("access-token"))
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("nss_refresh=refresh-token")));

    verify(authenticationManager).authenticate(any());
    verify(refreshSessions).issue(entity);
  }

  @Test
  void refreshReturnsNewAccessDataAndRotatesTheRefreshCookie() throws Exception {
    User user = new User();
    user.setId(7L);
    user.setEmail("user@nss.test");
    user.setPassword("encoded");
    user.setAccountVersion(0L);
    org.mockito.Mockito.when(refreshSessions.rotateWithToken("old-token"))
        .thenReturn(new RefreshSessionService.Rotation(user, "new-token"));
    org.mockito.Mockito.when(jwtService.generateToken(user)).thenReturn("new-access");

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "old-token")))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.token")
                .value("new-access"))
        .andExpect(
            header()
                .string(
                    "Set-Cookie", org.hamcrest.Matchers.containsString("nss_refresh=new-token")));

    verify(refreshSessions).rotateWithToken("old-token");
    verify(userService).entityById(user.getId());
  }

  @Test
  void meReturnsTheAuthenticatedUserAndAdminPermission() throws Exception {
    org.mockito.Mockito.when(userService.getByEmail("admin@nss.test"))
        .thenReturn(testUser(9L, "Admin", "admin@nss.test", true));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/auth/me")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("admin@nss.test")
                        .roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.email")
                .value("admin@nss.test"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.permissions[0]")
                .value("USER_MANAGEMENT"));

    verify(userService).getByEmail("admin@nss.test");
  }

  @Test
  void meOmitsAdministrativePermissionForARegularUser() throws Exception {
    org.mockito.Mockito.when(userService.getByEmail("user@nss.test"))
        .thenReturn(testUser(10L, "User", "user@nss.test", true));

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/auth/me")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("user@nss.test")
                        .roles("USER")))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.permissions")
                .isEmpty());
  }

  @Test
  void logoutWithoutRefreshCookieStillClearsTheClientCookie() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/logout").with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("nss_refresh=")));

    verify(refreshSessions).revoke(null);
  }

  @Test
  void adminLoginReturnsAdministrativePermission() throws Exception {
    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    User entity = new User();
    entity.setId(11L);
    entity.setEmail("admin@nss.test");
    entity.setPassword("encoded");
    entity.setAccountVersion(0L);
    entity.setAccountType(com.example.nss.user.entity.AccountType.ADMIN);
    UserDetails principal = new com.example.nss.user.security.VersionedUserDetails(entity);
    org.mockito.Mockito.when(authentication.getPrincipal()).thenReturn(principal);
    org.mockito.Mockito.when(authenticationManager.authenticate(any())).thenReturn(authentication);
    org.mockito.Mockito.when(jwtService.generateToken(entity)).thenReturn("admin-access");
    org.mockito.Mockito.when(userService.entityById(11L)).thenReturn(entity);
    org.mockito.Mockito.when(refreshSessions.issue(entity)).thenReturn("admin-refresh");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@nss.test\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.permissions[0]")
                .value("USER_MANAGEMENT"));
  }

  @Test
  void failedLoginReturnsUnauthorizedAndRecordsTheAttempt() throws Exception {
    org.mockito.Mockito.when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("invalid credentials"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());

    verify(rateLimiter).registerLoginAttempt("user@nss.test");
  }

  @Test
  void logoutIgnoresAnUnrelatedCookieBeforeClearingTheRefreshCookie() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(csrf())
                .cookie(
                    new jakarta.servlet.http.Cookie("session", "ignored"),
                    new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isNoContent());

    verify(refreshSessions).revoke("refresh-token");
  }

  private static User testUser(Long id, String name, String email, boolean active) {
    User user = new User();
    user.setId(id);
    user.setNome(name);
    user.setEmail(email);
    user.setAtivo(active);
    return user;
  }
}
