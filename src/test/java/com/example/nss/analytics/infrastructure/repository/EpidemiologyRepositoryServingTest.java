package com.example.nss.analytics.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "NSS_REAL_ANALYTICS_IT", matches = "true")
class EpidemiologyRepositoryServingTest {
  private static EpidemiologyRepository repository;
  private static JdbcTemplate jdbc;

  @BeforeAll
  static void connectToServingDatabase() {
    String url = required("ANALYTICS_DATASOURCE_URL");
    String username = required("ANALYTICS_DATASOURCE_USERNAME");
    String password = System.getenv().getOrDefault("ANALYTICS_DATASOURCE_PASSWORD", "");
    jdbc = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
    repository = new EpidemiologyRepository(jdbc, "ano", "mes");
  }

  @Test
  void discoversPublishedDiseasesAndMetadataFromServingDatabase() {
    var diseases = repository.diseases();
    var years = repository.metadata().stream().map(row -> row.year()).collect(Collectors.toSet());

    assertTrue(diseases.size() >= 31);
    assertTrue(years.containsAll(Set.of(2023, 2026)));
    assertTrue(diseases.stream().anyMatch(disease -> disease.code().equals("DENG")));
  }

  @Test
  void resolvesNationalMunicipalityDimensionAndUfAssociation() {
    var municipalities = repository.municipalities(null);
    var ufMunicipalities = repository.municipalities("33");

    assertTrue(municipalities.size() >= 5_000);
    assertFalse(ufMunicipalities.isEmpty());
    var municipality = ufMunicipalities.getFirst();
    assertTrue(repository.municipalityBelongsToUf(municipality.code(), "33"));
  }

  @Test
  void executesRealNationalAndStateAggregationsForPublishedDisease() {
    String municipalityCode =
        jdbc.queryForObject(
            "SELECT TRIM(cd_mun) FROM analytics.fato_casos "
                + "WHERE disease_codigo = 'DENG' AND ano = 2023 "
                + "GROUP BY cd_mun ORDER BY SUM(cases_total) DESC LIMIT 1",
            String.class);
    var filter =
        new EpidemiologyFilter("DENG", 2023, null, null, null, municipalityCode, null, null);

    var national = repository.totals(filter, Geography.MUNICIPALITY, List.of(municipalityCode));
    var states = repository.aggregate(filter, Geography.STATE, List.of());

    assertNotNull(national);
    assertTrue(national.total() > 0);
    assertTrue(national.mapped() <= national.total());
    assertFalse(states.isEmpty());
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
    return value;
  }
}
