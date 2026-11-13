package com.example.nss.analytics.infrastructure.cache;

import com.example.nss.analytics.api.response.AnalyticsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Best-effort response cache. Redis failures never make analytics unavailable. */
public final class AnalyticsResponseCache {
  private final StringRedisTemplate redis;
  private final ObjectMapper mapper;
  private final Duration ttl;
  private final String revision;

  public AnalyticsResponseCache(
      StringRedisTemplate redis, ObjectMapper mapper, Duration ttl, String revision) {
    this.redis = redis;
    this.mapper = mapper;
    this.ttl = ttl;
    this.revision = revision;
  }

  public AnalyticsResponse get(String requestKey) {
    try {
      String value = redis.opsForValue().get(key(requestKey));
      return value == null ? null : mapper.readValue(value, AnalyticsResponse.class);
    } catch (Exception ignored) {
      return null;
    }
  }

  public void put(String requestKey, AnalyticsResponse response) {
    try {
      String value = mapper.writeValueAsString(response);
      redis.opsForValue().set(key(requestKey), value, ttl);
    } catch (Exception ignored) {
      // Analytics remains available when the optional cache is unavailable.
    }
  }

  private String key(String requestKey) {
    byte[] digest = sha256((revision + ":" + requestKey).getBytes(StandardCharsets.UTF_8));
    return "nss:analytics:v1:" + HexFormat.of().formatHex(digest);
  }

  private static byte[] sha256(byte[] value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 indisponível", e);
    }
  }
}
