package com.example.nss.user.web.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.user.application.AuthenticationService;
import com.example.nss.user.application.query.AuthenticationResult;
import com.example.nss.user.web.request.LoginRequest;
import com.example.nss.user.web.response.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
  @Mock AuthenticationService authenticationService;
  @Mock HttpServletResponse response;
  @Mock HttpServletRequest request;
  @InjectMocks AuthController controller;

  @Test
  void delegatesLoginWithoutOrchestratingAuthentication() {
    when(authenticationService.login("  Ana@Example.COM ", "secret"))
        .thenReturn(result("access-token", "refresh-token"));
    ReflectionTestUtils.setField(controller, "refreshCookieName", "nss_refresh");
    ReflectionTestUtils.setField(controller, "refreshExpiration", 604_800_000L);

    controller.login(new LoginRequest("  Ana@Example.COM ", "secret"), response);

    verify(authenticationService).login("  Ana@Example.COM ", "secret");
  }

  @Test
  void propagatesFailedLogin() {
    when(authenticationService.login("user@example.com", "wrong"))
        .thenThrow(new BadCredentialsException("invalid"));

    assertThrows(
        BadCredentialsException.class,
        () -> controller.login(new LoginRequest("user@example.com", "wrong"), response));
  }

  @Test
  void propagatesInvalidAuthenticationResult() {
    when(authenticationService.login("user@example.com", "secret"))
        .thenThrow(new AuthenticationServiceException("invalid principal"));

    assertThrows(
        AuthenticationServiceException.class,
        () -> controller.login(new LoginRequest("user@example.com", "secret"), response));
  }

  @Test
  void delegatesRefreshAndSetsTheRotatedCookie() {
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("nss_refresh", "old-token")});
    when(authenticationService.refresh("old-token"))
        .thenReturn(result("new-access", "new-refresh"));
    ReflectionTestUtils.setField(controller, "refreshCookieName", "nss_refresh");
    ReflectionTestUtils.setField(controller, "refreshExpiration", 604_800_000L);

    controller.refresh(request, response);

    verify(authenticationService).refresh("old-token");
  }

  @Test
  void delegatesLogoutAndClearsTheCookie() {
    MockHttpServletRequest logoutRequest = new MockHttpServletRequest();
    logoutRequest.setCookies(new Cookie("nss_refresh", "refresh-token"));
    ReflectionTestUtils.setField(controller, "refreshCookieName", "nss_refresh");

    controller.logout(logoutRequest, response);

    verify(authenticationService).logout("refresh-token");
  }

  private AuthenticationResult result(String access, String refresh) {
    return new AuthenticationResult(
        access, refresh, new UserResponse(7L, "Ana", "ana@example.com", true), 3600, List.of());
  }
}
