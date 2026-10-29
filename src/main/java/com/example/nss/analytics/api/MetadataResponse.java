package com.example.nss.analytics.api;

import java.util.List;
import java.util.Map;

public record MetadataResponse(
    List<Integer> availableYears,
    Map<String, List<Integer>> availableMonthsByYear,
    List<String> supportedGeographies,
    List<String> supportedAgeBands,
    List<String> supportedSexes) {}
