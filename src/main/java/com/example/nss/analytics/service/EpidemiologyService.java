package com.example.nss.analytics.service;

import com.example.nss.analytics.entity.AnalyticsResult;
import com.example.nss.analytics.entity.CoverageStatus;
import com.example.nss.analytics.entity.EpidemiologyFilter;
import com.example.nss.analytics.entity.Geography;
import com.example.nss.analytics.entity.MetadataResult;
import com.example.nss.analytics.exception.MetadataCatalogUnavailableException;
import com.example.nss.analytics.repository.AnalyticsResponseCache;
import com.example.nss.analytics.repository.EpidemiologyRepository;
import com.example.nss.analytics.repository.EpidemiologyTotals;
import com.example.nss.analytics.repository.MetadataCatalogRow;
import com.example.nss.analytics.repository.PublicationMetadataRow;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpidemiologyService {
  private final EpidemiologyRepository repository;
  private final TerritoryCatalog catalog;
  private AnalyticsResponseCache cache;

  @Autowired(required = false)
  void setCache(AnalyticsResponseCache cache) {
    this.cache = cache;
  }

  public AnalyticsResult query(Geography geography, EpidemiologyFilter filter) {
    String cacheKey = cacheKey(geography, filter);
    if (cache != null) {
      AnalyticsResult cached = cache.get(cacheKey);
      if (cached != null) return cached;
    }
    if (geography == Geography.NEIGHBORHOOD && filter.districtCode() == null) {
      AnalyticsResult unavailable =
          buildResult(
              geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, null, List.of(), null);
      if (cache != null) cache.put(cacheKey, unavailable);
      return unavailable;
    }
    List<TerritoryCatalog.Entry> territories =
        geography == Geography.REGION || geography == Geography.STATE
            ? areaEntries(geography, filter)
            : catalog.forGeography(
                filter.municipalityCode(), filter.districtCode(), geography.name());
    if (geography == Geography.MUNICIPALITY && filter.ufCode() != null) {
      territories =
          territories.stream()
              .filter(entry -> entry.territoryId().startsWith(filter.ufCode()))
              .toList();
    }
    List<String> mappedIds = territories.stream().map(TerritoryCatalog.Entry::territoryId).toList();
    EpidemiologyTotals totals = repository.totals(filter, geography, mappedIds);
    long total = totals.total();
    long mapped = totals.mapped();
    List<AnalyticsResult.TerritoryItem> items = new ArrayList<>();
    Map<String, Long> aggregates = new HashMap<>();
    repository
        .aggregate(filter, geography, mappedIds)
        .forEach(row -> aggregates.put(row.code(), row.total()));
    for (TerritoryCatalog.Entry entry : territories) {
      items.add(
          new AnalyticsResult.TerritoryItem(
              entry.territoryId(),
              entry.name(),
              aggregates.getOrDefault(entry.territoryId(), 0L),
              entry.parentDistrictId()));
    }
    if (geography == Geography.NEIGHBORHOOD) {
      Set<String> knownCodes =
          territories.stream().map(TerritoryCatalog.Entry::territoryId).collect(Collectors.toSet());
      for (Map.Entry<String, Long> aggregate : aggregates.entrySet()) {
        String code = aggregate.getKey();
        if (!knownCodes.contains(code) && code.startsWith("CNES_BAIRRO:")) {
          items.add(
              new AnalyticsResult.TerritoryItem(
                  code,
                  nameOnlyNeighborhoodName(code),
                  aggregate.getValue(),
                  filter.districtCode()));
        }
      }
    }
    if (geography == Geography.MUNICIPALITY
        && filter.municipalityCode() == null
        && filter.ufCode() == null) {
      items = new ArrayList<>();
      for (TerritoryCatalog.Entry entry : catalog.forGeography(null, null, geography.name()))
        items.add(
            new AnalyticsResult.TerritoryItem(
                entry.territoryId(),
                entry.name(),
                aggregates.getOrDefault(entry.territoryId(), 0L),
                null));
    }
    items.sort(Comparator.comparing(AnalyticsResult.TerritoryItem::code));
    CoverageStatus status =
        total == 0 || mapped == total ? CoverageStatus.AVAILABLE : CoverageStatus.PARTIAL;
    EpidemiologyTotals unitTotals =
        geography == Geography.MUNICIPALITY ? repository.notificationUnitTotals(filter) : null;
    AnalyticsResult result =
        buildResult(
            geography,
            filter,
            total,
            status,
            mapped,
            total - mapped,
            unitTotals,
            items,
            repository.publicationMetadata(filter));
    if (cache != null) cache.put(cacheKey, result);
    return result;
  }

  private String nameOnlyNeighborhoodName(String code) {
    String[] parts = code.split(":", 3);
    if (parts.length < 3 || parts[2].isBlank()) return "Bairro informado no CNES";
    return URLDecoder.decode(parts[2], StandardCharsets.UTF_8);
  }

  private String cacheKey(Geography geography, EpidemiologyFilter f) {
    return String.join(
        "|",
        geography.name(),
        String.valueOf(f.disease()),
        String.valueOf(f.year()),
        String.valueOf(f.month()),
        String.valueOf(f.sex()),
        String.valueOf(f.ageBand()),
        String.valueOf(f.municipalityCode()),
        String.valueOf(f.districtCode()),
        String.valueOf(f.ufCode()),
        String.valueOf(f.regionCode()));
  }

  public AnalyticsResult unavailable(Geography geography, EpidemiologyFilter filter) {
    return buildResult(
        geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, null, List.of(), null);
  }

  private List<TerritoryCatalog.Entry> areaEntries(Geography geography, EpidemiologyFilter filter) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("N", "Norte");
    values.put("NE", "Nordeste");
    values.put("CO", "Centro-Oeste");
    values.put("SE", "Sudeste");
    values.put("S", "Sul");
    if (geography == Geography.REGION) {
      return values.entrySet().stream()
          .map(
              e ->
                  new TerritoryCatalog.Entry(
                      e.getKey(), e.getValue(), "REGION", e.getKey(), null, "AVAILABLE"))
          .toList();
    }
    Map<String, String> states =
        Map.ofEntries(
            Map.entry("11", "Rondônia"),
            Map.entry("12", "Acre"),
            Map.entry("13", "Amazonas"),
            Map.entry("14", "Roraima"),
            Map.entry("15", "Pará"),
            Map.entry("16", "Amapá"),
            Map.entry("17", "Tocantins"),
            Map.entry("21", "Maranhão"),
            Map.entry("22", "Piauí"),
            Map.entry("23", "Ceará"),
            Map.entry("24", "Rio Grande do Norte"),
            Map.entry("25", "Paraíba"),
            Map.entry("26", "Pernambuco"),
            Map.entry("27", "Alagoas"),
            Map.entry("28", "Sergipe"),
            Map.entry("29", "Bahia"),
            Map.entry("31", "Minas Gerais"),
            Map.entry("32", "Espírito Santo"),
            Map.entry("33", "Rio de Janeiro"),
            Map.entry("35", "São Paulo"),
            Map.entry("41", "Paraná"),
            Map.entry("42", "Santa Catarina"),
            Map.entry("43", "Rio Grande do Sul"),
            Map.entry("50", "Mato Grosso do Sul"),
            Map.entry("51", "Mato Grosso"),
            Map.entry("52", "Goiás"),
            Map.entry("53", "Distrito Federal"));
    return states.entrySet().stream()
        .filter(
            e -> filter.regionCode() == null || regionOf(e.getKey()).equals(filter.regionCode()))
        .map(
            e ->
                new TerritoryCatalog.Entry(
                    e.getKey(), e.getValue(), "STATE", e.getKey(), null, "AVAILABLE"))
        .toList();
  }

  private String regionOf(String uf) {
    if (uf.startsWith("1")) return "N";
    if (uf.startsWith("2")) return "NE";
    if (Set.of("31", "32", "33", "35").contains(uf)) return "SE";
    if (Set.of("41", "42", "43").contains(uf)) return "S";
    return "CO";
  }

  public List<String> diseases() {
    return repository.diseases().stream().map(row -> row.code()).toList();
  }

  public MetadataResult metadata() {
    List<MetadataCatalogRow> catalogRows = repository.metadataCatalog();
    if (catalogRows.isEmpty()) {
      throw new MetadataCatalogUnavailableException(
          "Catálogo de metadata epidemiológico ausente ou não publicado");
    }
    Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
    for (MetadataCatalogRow row : catalogRows) {
      List<Integer> months = grouped.computeIfAbsent(row.year(), ignored -> new ArrayList<>());
      row.availableMonths()
          .forEach(
              month -> {
                if (!months.contains(month)) months.add(month);
              });
    }
    grouped.values().forEach(values -> values.sort(Comparator.naturalOrder()));
    Map<Integer, List<Integer>> orderedByYear =
        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (left, right) -> left,
                    LinkedHashMap::new));
    return new MetadataResult(
        new ArrayList<>(orderedByYear.keySet()),
        orderedByYear.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()),
                    Map.Entry::getValue,
                    (left, right) -> left,
                    LinkedHashMap::new)),
        List.of("MUNICIPALITY", "DISTRICT", "NEIGHBORHOOD"),
        new ArrayList<>(EpidemiologyFilter.AGE_BANDS),
        List.of("M", "F", "ALL"),
        catalogRows.stream()
            .filter(MetadataCatalogRow::municipalityOnly)
            .map(row -> new MetadataResult.MunicipalityOnlyScope(row.disease(), row.year()))
            .toList());
  }

  private AnalyticsResult buildResult(
      Geography geography,
      EpidemiologyFilter f,
      Long total,
      CoverageStatus status,
      long mapped,
      long unmapped,
      EpidemiologyTotals unitTotals,
      List<AnalyticsResult.TerritoryItem> items,
      PublicationMetadataRow publication) {
    boolean municipalityOnly =
        unitTotals != null && unitTotals.total() > 0 && unitTotals.mapped() == 0;
    return new AnalyticsResult(
        "notifications",
        geography,
        new AnalyticsResult.FilterResponse(
            f.disease(),
            f.year(),
            f.month(),
            f.sex(),
            f.ageBand(),
            f.municipalityCode(),
            f.districtCode(),
            f.ufCode(),
            f.regionCode()),
        total,
        new AnalyticsResult.Coverage(
            status,
            mapped,
            unmapped,
            municipalityOnly ? "MUNICIPALITY_ONLY" : null,
            municipalityOnly ? "NOTIFICATION_UNIT_NOT_PROVIDED_BY_SOURCE" : null),
        items,
        publication == null ? null : publication.sourceExtractedAt(),
        publication == null ? null : publication.addedAt());
  }
}
