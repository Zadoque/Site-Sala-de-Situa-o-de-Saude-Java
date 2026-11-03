package com.example.nss.analytics.application;

import com.example.nss.analytics.application.port.EpidemiologyQueryPort;
import com.example.nss.analytics.application.port.TerritoryCatalogPort;
import com.example.nss.analytics.application.query.AnalyticsResult;
import com.example.nss.analytics.application.query.EpidemiologyTotals;
import com.example.nss.analytics.application.query.MetadataEntry;
import com.example.nss.analytics.application.query.MetadataResult;
import com.example.nss.analytics.application.query.Municipality;
import com.example.nss.analytics.domain.CoverageStatus;
import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpidemiologyService {
  private final EpidemiologyQueryPort repository;
  private final TerritoryCatalogPort catalog;

  public AnalyticsResult query(Geography geography, EpidemiologyFilter filter) {
    if (filter.municipalityCode() != null
        && filter.ufCode() != null
        && !repository.municipalityBelongsToUf(filter.municipalityCode(), filter.ufCode())) {
      throw new com.example.nss.analytics.exception.InvalidFilterException(
          "municipalityCode não pertence à UF informada");
    }
    List<TerritoryCatalogPort.Entry> territories =
        catalog.forGeography(filter.municipalityCode(), filter.districtCode(), geography.name());
    if (geography == Geography.MUNICIPALITY) {
      List<TerritoryCatalogPort.Entry> fileCatalog = territories;
      territories =
          repository.municipalities(null).stream()
              .filter(
                  m ->
                      filter.municipalityCode() == null
                          || m.code().equals(filter.municipalityCode()))
              .map(this::municipalityEntry)
              .toList();
      if (territories.isEmpty()) territories = fileCatalog;
    } else if (geography == Geography.NEIGHBORHOOD && territories.isEmpty()) {
      return unavailable(geography, filter);
    }
    List<String> mappedIds =
        territories.stream().map(TerritoryCatalogPort.Entry::territoryId).toList();
    EpidemiologyTotals totals = repository.totals(filter, geography, mappedIds);
    long total = totals.total();
    long mapped = totals.mapped();
    List<AnalyticsResult.Item> items = new ArrayList<>();
    Map<String, Long> aggregates = new HashMap<>();
    repository
        .aggregate(filter, geography, mappedIds)
        .forEach(row -> aggregates.put(row.code(), row.total()));
    if (geography == Geography.STATE || geography == Geography.REGION) {
      aggregates.forEach(
          (code, value) ->
              items.add(
                  new AnalyticsResult.Item(
                      code,
                      geography == Geography.REGION ? regionName(code) : stateName(code),
                      value,
                      null)));
    }
    for (TerritoryCatalogPort.Entry entry : territories) {
      items.add(
          new AnalyticsResult.Item(
              entry.territoryId(),
              entry.name(),
              aggregates.getOrDefault(entry.territoryId(), 0L),
              entry.parentDistrictId()));
    }
    items.sort(Comparator.comparing(AnalyticsResult.Item::code));
    CoverageStatus status =
        total == 0 || mapped == total ? CoverageStatus.AVAILABLE : CoverageStatus.PARTIAL;
    return response(geography, filter, total, status, mapped, total - mapped, items);
  }

  private TerritoryCatalogPort.Entry municipalityEntry(Municipality municipality) {
    return new TerritoryCatalogPort.Entry(
        municipality.code(),
        municipality.name(),
        "MUNICIPALITY",
        municipality.code(),
        null,
        "AVAILABLE");
  }

  private String regionName(String code) {
    return switch (code) {
      case "N" -> "Norte";
      case "NE" -> "Nordeste";
      case "SE" -> "Sudeste";
      case "S" -> "Sul";
      default -> "Centro-Oeste";
    };
  }

  private String stateName(String code) {
    return code;
  }

  public AnalyticsResult unavailable(Geography geography, EpidemiologyFilter filter) {
    return response(geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, List.of());
  }

  public List<String> diseases() {
    return repository.diseases().stream().map(row -> row.code()).toList();
  }

  public MetadataResult metadata() {
    Map<Integer, List<Integer>> grouped =
        repository.metadata().stream()
            .collect(
                Collectors.groupingBy(
                    MetadataEntry::year,
                    LinkedHashMap::new,
                    Collectors.mapping(MetadataEntry::month, Collectors.toList())));
    return new MetadataResult(
        new ArrayList<>(grouped.keySet()),
        grouped.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    Map.Entry::getValue,
                    (left, right) -> left,
                    LinkedHashMap::new)),
        List.of("MUNICIPALITY", "DISTRICT", "NEIGHBORHOOD"),
        new ArrayList<>(EpidemiologyFilter.AGE_BANDS),
        List.of("M", "F", "ALL"));
  }

  private AnalyticsResult response(
      Geography geography,
      EpidemiologyFilter f,
      Long total,
      CoverageStatus status,
      long mapped,
      long unmapped,
      List<AnalyticsResult.Item> items) {
    return new AnalyticsResult(
        "notifications",
        geography,
        new AnalyticsResult.Filters(
            f.disease(),
            f.year(),
            f.month(),
            f.sex(),
            f.ageBand(),
            f.municipalityCode(),
            f.districtCode(),
            f.ufCode()),
        total,
        new AnalyticsResult.Coverage(status, mapped, unmapped),
        items);
  }
}
