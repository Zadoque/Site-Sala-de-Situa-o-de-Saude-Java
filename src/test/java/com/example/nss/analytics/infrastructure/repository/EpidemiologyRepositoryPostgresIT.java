package com.example.nss.analytics.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            + "notification_territory_status VARCHAR, cases_total INT, cd_sexo VARCHAR, age_band VARCHAR)");
    jdbc.update(
        "INSERT INTO analytics.fato_casos VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "DENG",
        2024,
        3,
        "3301009",
        "CG_DIST_SEDE",
        "CG_NH_CENTRO",
        "NOTIFICATION_NEIGHBORHOOD",
        4,
        "F",
        "20_39");
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
}
