package com.example.nss.user.web;

import com.example.nss.shared.web.ErrorResponse;
import com.example.nss.user.domain.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.nss.user.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimit(
      RateLimitExceededException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(
            new ErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMITED",
                exception.getMessage(),
                request.getRequestURI(),
                UUID.randomUUID().toString()));
  }
}
