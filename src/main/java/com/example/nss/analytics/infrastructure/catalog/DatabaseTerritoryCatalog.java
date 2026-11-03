package com.example.nss.analytics.infrastructure.catalog;

import com.example.nss.analytics.application.port.TerritoryCatalogPort;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolves intramunicipal territory IDs from the serving facts published by the pipeline.
 *
 * <p>The pipeline does not publish a national district/neighborhood dimension. Consequently the
 * fact IDs are the only authoritative catalog for this level and names are intentionally not
 * invented: the published identifier is returned as its display name.
 */
@Component
@org.springframework.context.annotation.Primary
public class DatabaseTerritoryCatalog implements TerritoryCatalogPort {
  private static final String DISTRICT = "DISTRICT";
  private static final String NEIGHBORHOOD = "NEIGHBORHOOD";

  private final JdbcTemplate jdbc;

  public DatabaseTerritoryCatalog(@Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Entry> forGeography(String municipalityCode, String districtCode, String geography) {
    if (municipalityCode == null || municipalityCode.isBlank()) return List.of();
    if (DISTRICT.equals(geography)) {
      return jdbc.query(
          "SELECT DISTINCT TRIM(notification_district_id) AS territory_id "
              + "FROM analytics.fato_casos "
              + "WHERE TRIM(cd_mun) = ? "
              + "AND notification_district_id IS NOT NULL "
              + "AND notification_territory_status IN "
              + "('NOTIFICATION_DISTRICT_ONLY','NOTIFICATION_NEIGHBORHOOD') "
              + "ORDER BY territory_id",
          (rs, row) -> entry(rs.getString("territory_id"), municipalityCode, DISTRICT, null),
          municipalityCode);
    }
    if (NEIGHBORHOOD.equals(geography)) {
      if (districtCode == null || districtCode.isBlank()) return List.of();
      return jdbc.query(
          "SELECT DISTINCT TRIM(notification_neighborhood_id) AS territory_id "
              + "FROM analytics.fato_casos "
              + "WHERE TRIM(cd_mun) = ? "
              + "AND TRIM(notification_district_id) = ? "
              + "AND notification_neighborhood_id IS NOT NULL "
              + "AND notification_territory_status = 'NOTIFICATION_NEIGHBORHOOD' "
              + "ORDER BY territory_id",
          (rs, row) ->
              entry(rs.getString("territory_id"), municipalityCode, NEIGHBORHOOD, districtCode),
          municipalityCode,
          districtCode);
    }
    return List.of();
  }

  @Override
  public java.util.Optional<Entry> find(String id) {
    return java.util.Optional.empty();
  }

  private Entry entry(String id, String municipality, String type, String parentDistrict) {
    return new Entry(id, id, type, municipality, parentDistrict, "PUBLISHED");
  }
}
