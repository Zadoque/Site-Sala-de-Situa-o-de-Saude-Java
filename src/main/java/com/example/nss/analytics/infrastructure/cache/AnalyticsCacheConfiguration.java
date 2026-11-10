package com.example.nss.analytics.infrastructure.cache;

import com.example.nss.shared.redis.DedicatedRedis;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "analytics.cache.enabled", havingValue = "true")
public class AnalyticsCacheConfiguration {
  @Bean(destroyMethod = "close")
  DedicatedRedis analyticsCacheRedis(
      @Value("${analytics.cache.redis-url}") String url,
      @Value("${analytics.cache.timeout-ms:200}") long timeoutMs) {
    return new DedicatedRedis(url, Duration.ofMillis(timeoutMs));
  }

  @Bean
  AnalyticsResponseCache analyticsResponseCache(
      DedicatedRedis redis,
      @Value("${analytics.cache.ttl-seconds:600}") long ttlSeconds,
      @Value("${analytics.cache.revision:v1}") String revision) {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    return new AnalyticsResponseCache(
        redis.template(), mapper, Duration.ofSeconds(ttlSeconds), revision);
  }
}
