package com.example.nss.analytics.application.query;

import java.util.List;
import java.util.Map;

public record MetadataResult(
    List<Integer> availableYears,
    Map<String, List<Integer>> availableMonthsByYear,
    List<String> supportedGeographies,
    List<String> supportedAgeBands,
    List<String> supportedSexes) {}
