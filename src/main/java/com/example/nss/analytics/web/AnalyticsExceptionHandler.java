package com.example.nss.analytics.web;

import com.example.nss.analytics.domain.exception.InvalidFilterException;
import com.example.nss.shared.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.nss.analytics.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnalyticsExceptionHandler {
  @ExceptionHandler(InvalidFilterException.class)
  public ResponseEntity<ErrorResponse> handleFilter(
      InvalidFilterException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_FILTER",
                exception.getMessage(),
                request.getRequestURI(),
                UUID.randomUUID().toString()));
  }
}
