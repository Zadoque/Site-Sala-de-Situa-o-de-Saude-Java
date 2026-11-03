package com.example.nss.analytics.application.port;

import java.util.List;
import java.util.Optional;

public interface TerritoryCatalogPort {
  List<Entry> forGeography(String municipalityCode, String districtCode, String geography);

  Optional<Entry> find(String id);

  record Entry(
      String territoryId,
      String name,
      String type,
      String municipalityCode,
      String parentDistrictId,
      String geometryStatus) {}
}
