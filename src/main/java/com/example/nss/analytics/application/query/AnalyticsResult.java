package com.example.nss.analytics.application.query;

import com.example.nss.analytics.domain.CoverageStatus;
import com.example.nss.analytics.domain.Geography;
import java.util.List;

public record AnalyticsResult(
    String metric,
    Geography geography,
    Filters filters,
    Long totalNotifications,
    Coverage coverage,
    List<Item> items) {
  public record Filters(
      String disease,
      Integer year,
      Integer month,
      String sex,
      String ageBand,
      String municipalityCode,
      String districtCode,
      String ufCode) {
    public Filters(
        String disease,
        Integer year,
        Integer month,
        String sex,
        String ageBand,
        String municipalityCode,
        String districtCode) {
      this(disease, year, month, sex, ageBand, municipalityCode, districtCode, null);
    }
  }

  public record Coverage(
      CoverageStatus status, Long mappedNotificationsTotal, Long unmappedNotificationsTotal) {}

  public record Item(String code, String name, Long notificationsTotal, String parentDistrictId) {}
}
