package com.example.nss.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.nss.entity.AccountType;
import com.example.nss.entity.User;
import com.example.nss.repository.UserRepository;
import com.example.nss.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIntegrationIT {
  @Autowired MockMvc mockMvc;
  @Autowired JwtService jwtService;
  @Autowired UserRepository users;

  @Test
  void exposesLivenessAndReadinessProbesWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    mockMvc
        .perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void acceptsAValidJwtForAnAdminEndpoint() throws Exception {
    String email = "jwt-admin@nss.test";
    persistUser(email, AccountType.ADMIN, true);
    String token = jwtService.generateToken(details(email, "ADMIN"));

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsAnInvalidJwtAsAnonymous() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatesAUserButStillDeniesTheAdminEndpoint() throws Exception {
    String email = "jwt-user@nss.test";
    persistUser(email, AccountType.USER, true);
    String token = jwtService.generateToken(details(email, "USER"));

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsAValidJwtForADisabledUser() throws Exception {
    String email = "jwt-disabled@nss.test";
    persistUser(email, AccountType.USER, false);
    String token = jwtService.generateToken(details(email, "USER"));

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private void persistUser(String email, AccountType accountType, boolean active) {
    User user = new User();
    user.setEmail(email);
    user.setPassword("encoded-password");
    user.setAccountType(accountType);
    user.setAtivo(active);
    users.saveAndFlush(user);
    assertNotNull(users.findByEmail(email).orElse(null));
  }

  private UserDetails details(String email, String role) {
    return org.springframework.security.core.userdetails.User.withUsername(email)
        .password("ignored")
        .roles(role)
        .build();
  }
}
