package com.example.nss.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.nss.analytics.api.AnalyticsResponse;
import com.example.nss.analytics.api.EpidemiologyFilter;
import com.example.nss.analytics.api.Geography;
import com.example.nss.analytics.repository.DiseaseRow;
import com.example.nss.analytics.repository.EpidemiologyRepository;
import com.example.nss.analytics.repository.EpidemiologyTotals;
import com.example.nss.analytics.repository.MetadataRow;
import com.example.nss.analytics.repository.TerritoryAggregate;
import com.example.nss.analytics.service.EpidemiologyService;
import com.example.nss.analytics.service.TerritoryCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EpidemiologyServiceTest {
  @Mock EpidemiologyRepository repository;
  @Mock TerritoryCatalog catalog;
  @InjectMocks EpidemiologyService service;

  @Test
  void totalOfficialIsNotCalculatedFromVisibleItems() {
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(10L, 7L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(List.of(new TerritoryAggregate("CG_DIST_SEDE", 7L)));

    AnalyticsResponse response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", 2026, 3, null, null, "3301009", null));

    assertEquals(10L, response.totalNotifications());
    assertEquals(7L, response.coverage().mappedNotificationsTotal());
    assertEquals(3L, response.coverage().unmappedNotificationsTotal());
    assertEquals("PARTIAL", response.coverage().status().name());
  }

  @Test
  void neighborhoodWithoutDistrictIsUnavailable() {
    AnalyticsResponse response =
        service.query(
            Geography.NEIGHBORHOOD,
            new EpidemiologyFilter("DENG", null, null, null, null, "3301009", null));

    assertNull(response.totalNotifications());
    assertEquals("UNAVAILABLE", response.coverage().status().name());
    assertTrue(response.items().isEmpty());
    verifyNoInteractions(repository);
  }

  @Test
  void nonCamposIntramunicipalDrilldownIsUnavailable() {
    AnalyticsResponse response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", null, null, null, null, "3302205", null));

    assertNull(response.totalNotifications());
    assertEquals("UNAVAILABLE", response.coverage().status().name());
    verifyNoInteractions(repository);
  }

  @Test
  void availableZeroIsDifferentFromUnavailable() {
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any())).thenReturn(List.of());

    AnalyticsResponse response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", 2026, null, null, "20_39", "3301009", null));

    assertEquals(0L, response.totalNotifications());
    assertEquals("AVAILABLE", response.coverage().status().name());
  }

  @Test
  void metadataIsGroupedByYearAndDiseasesKeepRepositoryOrder() {
    when(repository.metadata())
        .thenReturn(
            List.of(new MetadataRow(2024, 1), new MetadataRow(2024, 2), new MetadataRow(2025, 1)));
    when(repository.diseases())
        .thenReturn(List.of(new DiseaseRow("DENG", "Dengue"), new DiseaseRow("COVID", "Covid-19")));

    assertEquals(List.of("DENG", "COVID"), service.diseases());
    assertEquals(List.of(2024, 2025), service.metadata().availableYears());
    assertEquals(List.of(1, 2), service.metadata().availableMonthsByYear().get("2024"));
    assertEquals(List.of(1), service.metadata().availableMonthsByYear().get("2025"));
  }

  @Test
  void municipalityWithoutFilterBuildsItemsFromTheCatalog() {
    List<TerritoryCatalog.Entry> municipalities =
        List.of(
            new TerritoryCatalog.Entry(
                "3301009", "Campos dos Goytacazes", "MUNICIPALITY", "3301009", null, "AVAILABLE"),
            new TerritoryCatalog.Entry(
                "3302403", "Macaé", "MUNICIPALITY", "3302403", null, "AVAILABLE"));
    when(catalog.forGeography(null, null, "MUNICIPALITY")).thenReturn(municipalities);
    when(repository.totals(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(new EpidemiologyTotals(5L, 5L));
    when(repository.aggregate(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(List.of(new TerritoryAggregate("3301009", 5L)));

    AnalyticsResponse response =
        service.query(
            Geography.MUNICIPALITY,
            new EpidemiologyFilter(null, null, null, null, null, null, null));

    assertEquals(2, response.items().size());
    assertEquals("3301009", response.items().getFirst().code());
    assertEquals(5L, response.items().getFirst().notificationsTotal());
    assertEquals(0L, response.items().get(1).notificationsTotal());
  }

  @Test
  void neighborhoodWithDistrictBuildsMappedItems() {
    TerritoryCatalog.Entry neighborhood =
        new TerritoryCatalog.Entry(
            "CG_NH_CENTRO",
            "Centro",
            "NEIGHBORHOOD_OR_LOCALITY",
            "3301009",
            "CG_DIST_SEDE",
            "AVAILABLE");
    when(catalog.forGeography("3301009", "CG_DIST_SEDE", "NEIGHBORHOOD"))
        .thenReturn(List.of(neighborhood));
    when(repository.totals(any(), eq(Geography.NEIGHBORHOOD), any()))
        .thenReturn(new EpidemiologyTotals(3L, 3L));
    when(repository.aggregate(any(), eq(Geography.NEIGHBORHOOD), any()))
        .thenReturn(List.of(new TerritoryAggregate("CG_NH_CENTRO", 3L)));

    AnalyticsResponse response =
        service.query(
            Geography.NEIGHBORHOOD,
            new EpidemiologyFilter(null, null, null, null, null, "3301009", "CG_DIST_SEDE"));

    assertEquals("AVAILABLE", response.coverage().status().name());
    assertEquals(1, response.items().size());
    assertEquals(3L, response.items().getFirst().notificationsTotal());
  }

  @Test
  void sortsTerritoryItemsByCodeBeforeReturningThem() {
    TerritoryCatalog.Entry first =
        new TerritoryCatalog.Entry("B", "Bairro B", "DISTRICT", "3301009", null, "AVAILABLE");
    TerritoryCatalog.Entry second =
        new TerritoryCatalog.Entry("A", "Bairro A", "DISTRICT", "3301009", null, "AVAILABLE");
    when(catalog.forGeography("3301009", null, "DISTRICT")).thenReturn(List.of(first, second));
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));

    AnalyticsResponse response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter(null, null, null, null, null, "3301009", null));

    assertEquals(List.of("A", "B"), response.items().stream().map(e -> e.code()).toList());
  }
}
