package com.example.nss.user.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final ObjectProvider<StringRedisTemplate> redis;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {

      filterChain.doFilter(request, response);
      return;
    }

    String token = authorizationHeader.substring(7);

    try {
      long userId = jwtService.extractUserId(token);

      if (SecurityContextHolder.getContext().getAuthentication() == null) {

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        if (userDetails instanceof VersionedUserDetails versioned
            && jwtService.isTokenValid(token, userId, userDetails.isEnabled())
            && jwtService.hasAccountRevision(token, versioned.accountRevision())
            && activeSession(token, userId)) {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
      // Token inválido: a requisição continuará anônima.
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private boolean activeSession(String token, long userId) {
    String tokenGeneration = jwtService.extractSessionGeneration(token);
    if (tokenGeneration == null) return true;
    StringRedisTemplate template = redis.getIfAvailable();
    if (template == null) return false;
    String activeGeneration = template.opsForValue().get("nss:auth:refresh:active:" + userId);
    return Objects.equals(tokenGeneration, activeGeneration);
  }
}
