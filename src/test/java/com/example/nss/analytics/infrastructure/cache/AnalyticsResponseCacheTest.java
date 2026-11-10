package com.example.nss.analytics.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.analytics.api.response.AnalyticsResponse;
import com.example.nss.analytics.domain.CoverageStatus;
import com.example.nss.analytics.domain.Geography;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AnalyticsResponseCacheTest {
  private final StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> values =
      org.mockito.Mockito.mock(ValueOperations.class);
  private final AnalyticsResponseCache cache =
      new AnalyticsResponseCache(
          redis, new ObjectMapper().findAndRegisterModules(), Duration.ofMinutes(1), "test");

  @Test
  void storesAndReadsAResponse() {
    AnalyticsResponse response = response();
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenReturn(json(response));

    cache.put("municipalities|DENG|2024", response);

    assertNotNull(cache.get("municipalities|DENG|2024"));
    verify(values).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void returnsMissWhenRedisHasNoValue() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenReturn(null);

    assertNull(cache.get("missing"));
  }

  @Test
  void failsOpenWhenRedisIsUnavailable() {
    when(redis.opsForValue()).thenReturn(values);
    doThrow(new IllegalStateException("redis down"))
        .when(values)
        .set(anyString(), anyString(), any(Duration.class));

    cache.put("unavailable", response());
    assertNull(cache.get("unavailable"));
  }

  private AnalyticsResponse response() {
    return new AnalyticsResponse(
        "notifications",
        Geography.MUNICIPALITY,
        new AnalyticsResponse.FilterResponse("DENG", 2024, null, null, null, null, null),
        1L,
        new AnalyticsResponse.Coverage(CoverageStatus.AVAILABLE, 1L, 0L),
        List.of(),
        null,
        null);
  }

  private String json(AnalyticsResponse value) {
    try {
      return new ObjectMapper().findAndRegisterModules().writeValueAsString(value);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
