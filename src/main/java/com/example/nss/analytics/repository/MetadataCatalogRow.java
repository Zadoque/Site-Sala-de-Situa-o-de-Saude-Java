package com.example.nss.analytics.repository;

import java.util.List;

/** Uma linha do catálogo epidemiológico publicado pela pipeline. */
public record MetadataCatalogRow(
    String disease,
    int year,
    List<Integer> availableMonths,
    boolean municipalityOnly) {}
