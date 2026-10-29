package com.example.nss.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.nss.security.JwtService;
import com.example.nss.service.AuthRateLimiter;
import com.example.nss.service.FirstAccessService;
import com.example.nss.service.RefreshSessionService;
import com.example.nss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerContractIT {
  @Autowired MockMvc mockMvc;

  @MockitoBean UserService userService;
  @MockitoBean AuthenticationManager authenticationManager;
  @MockitoBean UserDetailsService userDetailsService;
  @MockitoBean JwtService jwtService;
  @MockitoBean FirstAccessService firstAccessService;
  @MockitoBean RefreshSessionService refreshSessions;
  @MockitoBean AuthRateLimiter rateLimiter;

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
  void logoutRevokesTheCookieAndExpiresItOverHttp() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("nss_refresh", "refresh-token")))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

    verify(refreshSessions).revoke("refresh-token");
  }
}
