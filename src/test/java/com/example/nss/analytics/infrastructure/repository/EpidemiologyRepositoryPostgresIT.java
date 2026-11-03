package com.example.nss.analytics.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class EpidemiologyRepositoryPostgresIT {
  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  private JdbcTemplate jdbc;
  private EpidemiologyRepository repository;

  @BeforeEach
  void createSchema() {
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    jdbc.execute("DROP SCHEMA IF EXISTS analytics CASCADE");
    jdbc.execute("CREATE SCHEMA analytics");
    jdbc.execute(
        "CREATE TABLE analytics.fato_casos (disease_codigo VARCHAR, ano INT, mes INT, cd_mun VARCHAR, "
            + "notification_district_id VARCHAR, notification_neighborhood_id VARCHAR, "
            + "notification_territory_status VARCHAR, cases_total INT, cd_sexo VARCHAR, age_band VARCHAR, "
            + "batch_id VARCHAR)");
    jdbc.execute(
        "CREATE TABLE analytics.pipeline_publications (disease_codigo VARCHAR, ano INT, cd_mun CHAR(7), "
            + "batch_id VARCHAR, source_extracted_at TIMESTAMPTZ, published_at TIMESTAMPTZ)");
    jdbc.execute(
        "CREATE TABLE analytics.dim_municipio (cd_mun CHAR(7), cd_uf CHAR(2), nm_mun VARCHAR, nm_uf VARCHAR)");
    jdbc.update(
        "INSERT INTO analytics.dim_municipio VALUES (?, ?, ?, ?)", "3301009", "33", "Campos", "RJ");
    jdbc.update(
        "INSERT INTO analytics.pipeline_publications VALUES (?, ?, ?, ?, ?, ?)",
        "DENG",
        2024,
        "3301009",
        "batch-current",
        java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        java.time.OffsetDateTime.parse("2026-01-01T00:01:00Z"));
    jdbc.update(
        "INSERT INTO analytics.fato_casos VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "DENG",
        2024,
        3,
        "3301009",
        "CG_DIST_SEDE",
        "CG_NH_CENTRO",
        "NOTIFICATION_NEIGHBORHOOD",
        4,
        "F",
        "20_39",
        "batch-current");
    repository = new EpidemiologyRepository(jdbc, "ano", "mes");
  }

  @Test
  void executesParameterizedTotalsAndMetadataAgainstPostgres() {
    var filter = new EpidemiologyFilter("DENG", 2024, 3, "F", "20_39", "3301009", null);

    var totals = repository.totals(filter, Geography.DISTRICT, List.of("CG_DIST_SEDE"));

    assertEquals(4L, totals.total());
    assertEquals(4L, totals.mapped());
    assertEquals(List.of(2024), repository.metadata().stream().map(row -> row.year()).toList());
  }

  @Test
  void ignoresFactsFromAnOlderPublicationSnapshot() {
    jdbc.update(
        "INSERT INTO analytics.pipeline_publications VALUES (?, ?, ?, ?, ?, ?)",
        "DENG",
        2024,
        "3301009",
        "batch-old",
        java.time.OffsetDateTime.parse("2025-01-01T00:00:00Z"),
        java.time.OffsetDateTime.parse("2025-01-01T00:01:00Z"));
    jdbc.update(
        "INSERT INTO analytics.fato_casos VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "DENG",
        2024,
        3,
        "3301009",
        "CG_DIST_SEDE",
        "CG_NH_CENTRO",
        "NOTIFICATION_NEIGHBORHOOD",
        99,
        "F",
        "20_39",
        "batch-old");

    var filter = new EpidemiologyFilter("DENG", 2024, 3, "F", "20_39", "3301009", null);

    assertEquals(
        4L, repository.totals(filter, Geography.DISTRICT, List.of("CG_DIST_SEDE")).total());
  }

  @Test
  void appliesUfFilterAgainstTheOfficialMunicipalityDimension() {
    var filter = new EpidemiologyFilter("DENG", 2024, 3, "F", "20_39", null, null, "33");

    assertTrue(repository.municipalityBelongsToUf("3301009", "33"));
    assertEquals(4L, repository.totals(filter, Geography.STATE, List.of()).total());
  }
}
