package com.example.nss.user.application;

import com.example.nss.user.application.command.FirstAccessCommand;
import com.example.nss.user.application.command.PasswordResetCommand;
import com.example.nss.user.application.port.AccessTokenPort;
import com.example.nss.user.application.port.AuthenticationGateway;
import com.example.nss.user.application.query.AuthenticationResult;
import com.example.nss.user.application.query.UserView;
import com.example.nss.user.domain.AccountType;
import com.example.nss.user.domain.UserAccount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
  private final AuthenticationGateway authenticationGateway;
  private final AccessTokenPort accessToken;
  private final RefreshSessionService refreshSessions;
  private final UserService users;
  private final FirstAccessService firstAccess;
  private final AuthRateLimiter rateLimiter;

  @Value("${jwt.expiration:3600000}")
  private long accessExpirationMillis;

  public AuthenticationResult login(String email, String password) {
    rateLimiter.registerLoginAttempt(email);
    AuthenticationGateway.AuthenticatedUser authenticated =
        authenticationGateway.authenticate(email, password);
    rateLimiter.loginSucceeded(email);
    UserAccount user = authenticated.user();
    return result(user, refreshSessions.issue(user));
  }

  public AuthenticationResult refresh(String rawRefreshToken) {
    RefreshSessionService.Rotation rotation = refreshSessions.rotateWithToken(rawRefreshToken);
    return result(rotation.user(), rotation.rawToken());
  }

  public void logout(String rawRefreshToken) {
    refreshSessions.revoke(rawRefreshToken);
  }

  public void completeFirstAccess(FirstAccessCommand command) {
    firstAccess.complete(command);
  }

  public void requestPasswordReset(String email) {
    rateLimiter.registerPasswordResetRequest(email);
    users.requestPasswordReset(email);
  }

  public void completePasswordReset(PasswordResetCommand command) {
    firstAccess.completePasswordReset(command);
  }

  public MeResult me(String email) {
    UserAccount account = users.entityByEmail(email);
    return new MeResult(users.getByEmail(email), permissions(account));
  }

  private AuthenticationResult result(UserAccount user, String refreshToken) {
    UserView view = users.getByEmail(user.getEmail());
    return new AuthenticationResult(
        accessToken.generateToken(user.getId()),
        refreshToken,
        view,
        accessExpirationMillis / 1000,
        permissions(user));
  }

  private List<String> permissions(UserAccount user) {
    return user.getAccountType() == AccountType.ADMIN ? List.of("USER_MANAGEMENT") : List.of();
  }

  public record MeResult(UserView user, List<String> permissions) {}
}
