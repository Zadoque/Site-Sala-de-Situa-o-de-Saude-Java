package com.example.nss.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.nss.security.JwtService;
import com.example.nss.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserAuthorizationIT {
  @Autowired MockMvc mockMvc;

  @MockitoBean AdminUserService service;
  @MockitoBean JwtService jwtService;
  @MockitoBean UserDetailsService userDetailsService;

  @Test
  void rejectsAnonymousAccessToAdminUsers() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsAuthenticatedNonAdminAccessToAdminUsers() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/users").with(user("user").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void permitsAuthenticatedAdminAccessToAdminUsers() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk());
  }
}
