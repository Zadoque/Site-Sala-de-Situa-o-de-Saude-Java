package com.example.demo.analytics.api;

import java.util.List;

public record AnalyticsResponse(String metric, Geography geography, FilterResponse filters,
                                Long totalNotifications, Coverage coverage, List<TerritoryItem> items) {
    public record FilterResponse(String disease, Integer year, Integer month, String sex, String ageBand,
                                 String municipalityCode, String districtCode) {}
    public record Coverage(CoverageStatus status, Long mappedNotificationsTotal, Long unmappedNotificationsTotal) {}
    public record TerritoryItem(String code, String name, Long notificationsTotal, String parentDistrictId) {}
}
