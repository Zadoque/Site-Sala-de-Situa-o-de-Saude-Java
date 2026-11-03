package com.example.nss.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.nss.user.application.port.AccessTokenPort;
import com.example.nss.user.application.port.AuthenticationGateway;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.domain.AccountType;
import com.example.nss.user.domain.UserAccount;
import com.example.nss.user.web.response.UserResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
  @Mock AuthenticationGateway authenticationGateway;
  @Mock AccessTokenPort accessToken;
  @Mock RefreshSessionService refreshSessions;
  @Mock UserService users;
  @Mock FirstAccessService firstAccess;
  @Mock AuthRateLimiter rateLimiter;

  AuthenticationService service;

  @BeforeEach
  void setUp() {
    service =
        new AuthenticationService(
            authenticationGateway, accessToken, refreshSessions, users, firstAccess, rateLimiter);
    ReflectionTestUtils.setField(service, "accessExpirationMillis", 3_600_000L);
  }

  @Test
  void loginIssuesAccessAndRefreshCredentialsAndPermissions() {
    UserAccount user = user(7L, "admin@nss.test", AccountType.ADMIN);
    when(authenticationGateway.authenticate("admin@nss.test", "secret"))
        .thenReturn(new AuthenticationGateway.AuthenticatedUser(user));
    when(refreshSessions.issue(user)).thenReturn("refresh");
    when(accessToken.generateToken(7L)).thenReturn("access");
    UserView view = new UserResponse(7L, "Admin", "admin@nss.test", true);
    when(users.getByEmail("admin@nss.test")).thenReturn(view);

    var result = service.login("admin@nss.test", "secret");

    assertEquals("access", result.accessToken());
    assertEquals("refresh", result.refreshToken());
    assertEquals(3600, result.accessExpirationSeconds());
    assertEquals(List.of("USER_MANAGEMENT"), result.permissions());
    verify(rateLimiter).registerLoginAttempt("admin@nss.test");
    verify(rateLimiter).loginSucceeded("admin@nss.test");
  }

  @Test
  void refreshRotatesTheSessionAndIssuesANewAccessToken() {
    UserAccount user = user(3L, "user@nss.test", AccountType.USER);
    when(refreshSessions.rotateWithToken("old"))
        .thenReturn(new RefreshSessionService.Rotation(user, "new"));
    when(accessToken.generateToken(3L)).thenReturn("access");
    when(users.getByEmail("user@nss.test"))
        .thenReturn(new UserResponse(3L, "User", "user@nss.test", true));

    var result = service.refresh("old");

    assertEquals("access", result.accessToken());
    assertEquals("new", result.refreshToken());
    assertEquals(List.of(), result.permissions());
    verify(refreshSessions).rotateWithToken("old");
  }

  @Test
  void delegatesLogoutAndPasswordFlows() {
    service.logout("refresh");
    service.requestPasswordReset("user@nss.test");

    verify(refreshSessions).revoke("refresh");
    verify(rateLimiter).registerPasswordResetRequest("user@nss.test");
    verify(users).requestPasswordReset("user@nss.test");
    verifyNoInteractions(authenticationGateway, accessToken, firstAccess);
  }

  @Test
  void returnsUserAndPermissionsForMe() {
    UserAccount user = user(4L, "admin@nss.test", AccountType.ADMIN);
    UserView view = new UserResponse(4L, "Admin", "admin@nss.test", true);
    when(users.entityByEmail("admin@nss.test")).thenReturn(user);
    when(users.getByEmail("admin@nss.test")).thenReturn(view);

    var result = service.me("admin@nss.test");

    assertEquals(view, result.user());
    assertEquals(List.of("USER_MANAGEMENT"), result.permissions());
  }

  private UserAccount user(Long id, String email, AccountType type) {
    UserAccount user = new UserAccount();
    user.setId(id);
    user.setEmail(email);
    user.setAccountType(type);
    return user;
  }
}
