package com.example.nss.analytics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.analytics.dto.mapper.AnalyticsMapper;
import com.example.nss.analytics.entity.AnalyticsResult;
import com.example.nss.analytics.entity.Geography;
import com.example.nss.analytics.entity.MetadataResult;
import com.example.nss.analytics.service.EpidemiologyService;
import java.util.List;
import org.junit.jupiter.api.Test;

class EpidemiologyControllerTest {
  private final EpidemiologyService service = mock(EpidemiologyService.class);
  private final EpidemiologyController controller =
      new EpidemiologyController(service, new AnalyticsMapper());
  private final EpidemiologyController.FilterRequest request =
      new EpidemiologyController.FilterRequest(
          "DENG", 2024, 3, "F", "20_39", "3301009", "CG_DIST_SEDE");

  @Test
  void regionAndStateDelegateToAggregatedResponses() {
    AnalyticsResult response =
        new AnalyticsResult(
            "notifications",
            Geography.REGION,
            new AnalyticsResult.FilterResponse(
                null, null, null, null, null, null, null, null, null),
            0L,
            new AnalyticsResult.Coverage(
                com.example.nss.analytics.entity.CoverageStatus.AVAILABLE, 0L, 0L),
            List.of(),
            null,
            null);
    when(service.query(Geography.REGION, request.toFilter())).thenReturn(response);
    when(service.query(Geography.STATE, request.toFilter())).thenReturn(response);

    assertEquals(response.metric(), controller.region(request).metric());
    assertEquals(response.metric(), controller.state(request).metric());
    verify(service).query(Geography.REGION, request.toFilter());
    verify(service).query(Geography.STATE, request.toFilter());
  }

  @Test
  void geographicEndpointsDelegateToTheMatchingServiceScope() {
    AnalyticsResult response =
        new AnalyticsResult(
            "notifications",
            Geography.MUNICIPALITY,
            new AnalyticsResult.FilterResponse(
                null, null, null, null, null, null, null, null, null),
            0L,
            new AnalyticsResult.Coverage(
                com.example.nss.analytics.entity.CoverageStatus.AVAILABLE, 0L, 0L),
            List.of(),
            null,
            null);
    when(service.query(Geography.MUNICIPALITY, request.toFilter())).thenReturn(response);
    when(service.query(Geography.DISTRICT, request.toFilter())).thenReturn(response);
    when(service.query(Geography.NEIGHBORHOOD, request.toFilter())).thenReturn(response);

    assertEquals(response.metric(), controller.municipalities(request).metric());
    assertEquals(response.metric(), controller.districts(request).metric());
    assertEquals(response.metric(), controller.neighborhoods(request).metric());
    verify(service).query(Geography.MUNICIPALITY, request.toFilter());
    verify(service).query(Geography.DISTRICT, request.toFilter());
    verify(service).query(Geography.NEIGHBORHOOD, request.toFilter());
  }

  @Test
  void diseasesAndMetadataUseTypedResponses() {
    MetadataResult metadata =
        new MetadataResult(
            List.of(2024), java.util.Map.of("2024", List.of(3)), List.of(), List.of(), List.of());
    when(service.diseases()).thenReturn(List.of("DENG"));
    when(service.metadata()).thenReturn(metadata);

    assertEquals(List.of("DENG"), controller.diseases().items());
    assertEquals(List.of(2024), controller.metadata().availableYears());
  }
}
