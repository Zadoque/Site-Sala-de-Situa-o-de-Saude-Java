package com.example.nss.analytics.entity;

import java.util.List;
import java.util.Map;

public record MetadataResult(
    List<Integer> availableYears,
    Map<String, List<Integer>> availableMonthsByYear,
    List<String> supportedGeographies,
    List<String> supportedAgeBands,
    List<String> supportedSexes,
    List<MunicipalityOnlyScope> municipalityOnlyScopes) {
  public MetadataResult(
      List<Integer> availableYears,
      Map<String, List<Integer>> availableMonthsByYear,
      List<String> supportedGeographies,
      List<String> supportedAgeBands,
      List<String> supportedSexes) {
    this(
        availableYears,
        availableMonthsByYear,
        supportedGeographies,
        supportedAgeBands,
        supportedSexes,
        List.of());
  }

  public record MunicipalityOnlyScope(String disease, Integer year) {}
}
