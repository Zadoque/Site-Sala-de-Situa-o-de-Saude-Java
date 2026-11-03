package com.example.nss.analytics.infrastructure.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class FileTerritoryCatalog
    implements com.example.nss.analytics.application.port.TerritoryCatalogPort {
  private static final String DISTRICT = "DISTRICT";
  private static final String NEIGHBORHOOD = "NEIGHBORHOOD";
  private static final String MUNICIPALITY = "MUNICIPALITY";
  private static final String NEIGHBORHOOD_OR_LOCALITY = "NEIGHBORHOOD_OR_LOCALITY";
  private static final int MINIMUM_TERRITORY_COLUMNS = 6;
  private final Map<String, com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry>
      entries;

  public FileTerritoryCatalog() {
    this.entries = load();
  }

  public List<com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry> forGeography(
      String municipalityCode, String districtCode, String geography) {
    if (DISTRICT.equals(geography))
      return entries.values().stream()
          .filter(
              e ->
                  DISTRICT.equals(e.type())
                      && Objects.equals(municipalityCode, e.municipalityCode()))
          .toList();
    if (NEIGHBORHOOD.equals(geography))
      return entries.values().stream()
          .filter(
              e ->
                  NEIGHBORHOOD_OR_LOCALITY.equals(e.type())
                      && Objects.equals(municipalityCode, e.municipalityCode())
                      && Objects.equals(districtCode, e.parentDistrictId()))
          .toList();
    return List.of();
  }

  public Optional<com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry> find(
      String id) {
    return Optional.ofNullable(entries.get(id));
  }

  private Map<String, com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry>
      load() {
    Map<String, com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry> result =
        new LinkedHashMap<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource("territories/campos.tsv").getInputStream(),
                StandardCharsets.UTF_8))) {
      reader.readLine();
      String line = reader.readLine();
      while (line != null) {
        String[] p = line.split("\\t", -1);
        if (p.length >= MINIMUM_TERRITORY_COLUMNS) {
          result.put(
              p[0],
              new com.example.nss.analytics.application.port.TerritoryCatalogPort.Entry(
                  p[0], p[1], p[2], p[3], p[4].isBlank() ? null : p[4], p[5]));
        }
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Catálogo territorial não disponível", e);
    }
    return Map.copyOf(result);
  }

  public record Entry(
      String territoryId,
      String name,
      String type,
      String municipalityCode,
      String parentDistrictId,
      String geometryStatus) {}
}
