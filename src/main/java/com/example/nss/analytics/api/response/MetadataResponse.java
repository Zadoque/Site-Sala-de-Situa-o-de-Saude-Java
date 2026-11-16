package com.example.nss.analytics.api.response;

import java.util.List;
import java.util.Map;

public record MetadataResponse(
    List<Integer> availableYears,
    Map<String, List<Integer>> availableMonthsByYear,
    List<String> supportedGeographies,
    List<String> supportedAgeBands,
    List<String> supportedSexes,
    List<MunicipalityOnlyScope> municipalityOnlyScopes) {
  public MetadataResponse(
      List<Integer> availableYears,
      Map<String, List<Integer>> availableMonthsByYear,
      List<String> supportedGeographies,
      List<String> supportedAgeBands,
      List<String> supportedSexes) {
    this(availableYears, availableMonthsByYear, supportedGeographies, supportedAgeBands, supportedSexes, List.of());
  }

  public record MunicipalityOnlyScope(String disease, Integer year) {}
}
