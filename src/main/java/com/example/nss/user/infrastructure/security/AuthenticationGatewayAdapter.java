package com.example.nss.user.infrastructure.security;

import com.example.nss.user.application.port.AuthenticationGateway;
import com.example.nss.user.application.port.UserGateway;
import com.example.nss.user.domain.rule.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationGatewayAdapter implements AuthenticationGateway {
  private final AuthenticationManager authenticationManager;
  private final UserGateway users;

  @Override
  public AuthenticatedUser authenticate(String email, String password) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(EmailNormalizer.normalize(email), password));
    if (!(authentication.getPrincipal() instanceof UserDetails details)) {
      throw new AuthenticationServiceException("A autenticação não retornou um usuário válido");
    }
    return new AuthenticatedUser(
        users
            .findByEmail(EmailNormalizer.normalize(details.getUsername()))
            .orElseThrow(
                () -> new AuthenticationServiceException("Usuário autenticado não encontrado")));
  }
}
