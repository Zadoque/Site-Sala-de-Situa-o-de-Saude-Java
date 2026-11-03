package com.example.nss.user.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.nss.user.application.AuthenticationService;
import com.example.nss.user.application.query.AuthenticationResult;
import com.example.nss.user.infrastructure.security.JwtService;
import com.example.nss.user.web.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
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

  @MockitoBean AuthenticationService authenticationService;
  @MockitoBean JwtService accessToken;

  @Test
  void rejectsInvalidFirstAccessPayloadBeforeCallingTheUseCase() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/first-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"invalid\",\"name\":\"\","
                        + "\"password\":\"short\",\"token\":\"\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(authenticationService);
  }

  @Test
  void delegatesValidFirstAccess() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/first-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"user@nss.test\",\"name\":\"User\","
                        + "\"password\":\"Password123!\",\"token\":\"token\"}"))
        .andExpect(status().isNoContent());
    verify(authenticationService).completeFirstAccess(any());
  }

  @Test
  void delegatesPasswordResetRequestWithoutRevealingAccountExistence() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\"}"))
        .andExpect(status().isAccepted());
    verify(authenticationService).requestPasswordReset("user@nss.test");
  }

  @Test
  void exposesCsrfTokenForBrowserClients() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")));
  }

  @Test
  void rejectsRefreshWithoutCsrfToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(authenticationService);
  }

  @Test
  void allowsPatchInConfiguredCorsContract() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/admin/users/1")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void loginReturnsAccessDataAndSetsRefreshCookie() throws Exception {
    when(authenticationService.login("user@nss.test", "Password123!"))
        .thenReturn(result("access-token", "refresh-token", false));
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("access-token"))
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("nss_refresh=refresh-token")));
    verify(authenticationService).login("user@nss.test", "Password123!");
  }

  @Test
  void refreshReturnsNewAccessDataAndRotatesCookie() throws Exception {
    when(authenticationService.refresh("old-token"))
        .thenReturn(result("new-access", "new-token", false));
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "old-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("new-access"))
        .andExpect(
            header()
                .string(
                    "Set-Cookie", org.hamcrest.Matchers.containsString("nss_refresh=new-token")));
    verify(authenticationService).refresh("old-token");
  }

  @Test
  void meReturnsAdministrativePermission() throws Exception {
    UserResponse user = new UserResponse(9L, "Admin", "admin@nss.test", true);
    when(authenticationService.me("admin@nss.test"))
        .thenReturn(new AuthenticationService.MeResult(user, java.util.List.of("USER_MANAGEMENT")));
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("admin@nss.test")
                        .roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@nss.test"))
        .andExpect(jsonPath("$.permissions[0]").value("USER_MANAGEMENT"));
  }

  @Test
  void meOmitsAdministrativePermissionForRegularUser() throws Exception {
    UserResponse user = new UserResponse(10L, "User", "user@nss.test", true);
    when(authenticationService.me("user@nss.test"))
        .thenReturn(new AuthenticationService.MeResult(user, java.util.List.of()));
    mockMvc
        .perform(
            get("/api/v1/auth/me")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("user@nss.test")
                        .roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions").isEmpty());
  }

  @Test
  void logoutWithoutRefreshCookieStillClearsClientCookie() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout").with(csrf())).andExpect(status().isNoContent());
    verify(authenticationService).logout(null);
  }

  @Test
  void adminLoginReturnsAdministrativePermission() throws Exception {
    when(authenticationService.login("admin@nss.test", "Password123!"))
        .thenReturn(result("admin-access", "admin-refresh", true));
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@nss.test\",\"password\":\"Password123!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions[0]").value("USER_MANAGEMENT"));
  }

  @Test
  void failedLoginReturnsUnauthorized() throws Exception {
    when(authenticationService.login("user@nss.test", "wrong"))
        .thenThrow(new BadCredentialsException("invalid credentials"));
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@nss.test\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutIgnoresUnrelatedCookie() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(csrf())
                .cookie(
                    new jakarta.servlet.http.Cookie("session", "ignored"),
                    new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isNoContent());
    verify(authenticationService).logout("refresh-token");
  }

  private AuthenticationResult result(String access, String refresh, boolean admin) {
    return new AuthenticationResult(
        access,
        refresh,
        new UserResponse(
            7L, admin ? "Admin" : "User", admin ? "admin@nss.test" : "user@nss.test", true),
        3600,
        admin ? java.util.List.of("USER_MANAGEMENT") : java.util.List.of());
  }
}
