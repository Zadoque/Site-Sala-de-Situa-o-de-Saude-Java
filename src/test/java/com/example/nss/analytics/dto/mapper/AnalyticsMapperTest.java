package com.example.nss.analytics.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.nss.analytics.entity.AnalyticsResult;
import com.example.nss.analytics.entity.CoverageStatus;
import com.example.nss.analytics.entity.Geography;
import com.example.nss.analytics.entity.MetadataResult;
import com.example.nss.analytics.repository.CasoResumo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalyticsMapperTest {
  private final AnalyticsMapper mapper = new AnalyticsMapper();

  @Test
  void mapsAnalyticsResultToHttpResponse() {
    var result =
        new AnalyticsResult(
            "notifications",
            Geography.DISTRICT,
            new AnalyticsResult.FilterResponse("DENG", 2026, 1, null, null, "3301009", null),
            10L,
            new AnalyticsResult.Coverage(CoverageStatus.PARTIAL, 7L, 3L, "DISTRICT", "PARTIAL"),
            List.of(new AnalyticsResult.TerritoryItem("D1", "Centro", 7L, null)),
            null,
            null);

    var response = mapper.toResponse(result);

    assertEquals("notifications", response.metric());
    assertEquals("D1", response.items().getFirst().code());
    assertEquals("PARTIAL", response.coverage().status().name());
  }

  @Test
  void mapsMetadataAndCasoResumoResults() {
    var metadata =
        mapper.toResponse(
            new MetadataResult(
                List.of(2026),
                Map.of("2026", List.of(1)),
                List.of("MUNICIPALITY"),
                List.of("ALL"),
                List.of("ALL"),
                List.of(new MetadataResult.MunicipalityOnlyScope("SIFA", 2026))));
    var caso =
        mapper.toResponse(
            new CasoResumo("DENG", 2026, 1, "3301009", "U1", "C", "A", "F", 1, 1990, 2));

    assertEquals(List.of(2026), metadata.availableYears());
    assertEquals("SIFA", metadata.municipalityOnlyScopes().getFirst().disease());
    assertEquals("DENG", caso.codigoDoenca());
    assertEquals(2, caso.totalCasos());
  }
}
