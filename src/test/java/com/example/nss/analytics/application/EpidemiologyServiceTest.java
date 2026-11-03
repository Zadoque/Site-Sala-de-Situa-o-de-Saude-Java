package com.example.nss.analytics.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.nss.analytics.application.port.TerritoryCatalogPort;
import com.example.nss.analytics.application.query.AnalyticsResult;
import com.example.nss.analytics.application.query.Disease;
import com.example.nss.analytics.application.query.EpidemiologyTotals;
import com.example.nss.analytics.application.query.MetadataEntry;
import com.example.nss.analytics.application.query.TerritoryAggregate;
import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import com.example.nss.analytics.infrastructure.repository.EpidemiologyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EpidemiologyServiceTest {
  @Mock EpidemiologyRepository repository;
  @Mock TerritoryCatalogPort catalog;
  @InjectMocks EpidemiologyService service;

  @Test
  void totalOfficialIsNotCalculatedFromVisibleItems() {
    when(catalog.forGeography("3301009", null, "DISTRICT"))
        .thenReturn(
            List.of(
                new TerritoryCatalogPort.Entry(
                    "CG_DIST_SEDE", "Sede", "DISTRICT", "3301009", null, "AVAILABLE")));
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(10L, 7L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(List.of(new TerritoryAggregate("CG_DIST_SEDE", 7L)));

    AnalyticsResult response =
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
    AnalyticsResult response =
        service.query(
            Geography.NEIGHBORHOOD,
            new EpidemiologyFilter("DENG", null, null, null, null, "3301009", null));

    assertNull(response.totalNotifications());
    assertEquals("UNAVAILABLE", response.coverage().status().name());
    assertTrue(response.items().isEmpty());
    verifyNoInteractions(repository);
  }

  @Test
  void nationalCatalogDoesNotRejectAValidMunicipality() {
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any())).thenReturn(List.of());
    AnalyticsResult response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", null, null, null, null, "3302205", null));

    assertEquals(0L, response.totalNotifications());
    assertEquals("AVAILABLE", response.coverage().status().name());
  }

  @Test
  void unavailableCreatesAnExplicitUnavailableResponse() {
    AnalyticsResult response =
        service.unavailable(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", 2026, null, null, null, "3301009", null));

    assertNull(response.totalNotifications());
    assertEquals("UNAVAILABLE", response.coverage().status().name());
    assertTrue(response.items().isEmpty());
  }

  @Test
  void availableZeroIsDifferentFromUnavailable() {
    when(catalog.forGeography("3301009", null, "DISTRICT"))
        .thenReturn(
            List.of(
                new TerritoryCatalogPort.Entry(
                    "CG_DIST_SEDE", "Sede", "DISTRICT", "3301009", null, "AVAILABLE")));
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any())).thenReturn(List.of());

    AnalyticsResult response =
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
            List.of(
                new MetadataEntry(2024, 1),
                new MetadataEntry(2024, 2),
                new MetadataEntry(2025, 1)));
    when(repository.diseases())
        .thenReturn(List.of(new Disease("DENG", "Dengue"), new Disease("COVID", "Covid-19")));

    assertEquals(List.of("DENG", "COVID"), service.diseases());
    assertEquals(List.of(2024, 2025), service.metadata().availableYears());
    assertEquals(List.of(1, 2), service.metadata().availableMonthsByYear().get("2024"));
    assertEquals(List.of(1), service.metadata().availableMonthsByYear().get("2025"));
  }

  @Test
  void municipalityWithoutFilterBuildsItemsFromTheCatalog() {
    List<TerritoryCatalogPort.Entry> municipalities =
        List.of(
            new TerritoryCatalogPort.Entry(
                "3301009", "Campos dos Goytacazes", "MUNICIPALITY", "3301009", null, "AVAILABLE"),
            new TerritoryCatalogPort.Entry(
                "3302403", "Macaé", "MUNICIPALITY", "3302403", null, "AVAILABLE"));
    when(catalog.forGeography(null, null, "MUNICIPALITY"))
        .thenReturn(List.of(municipalities.getFirst()), municipalities);
    when(repository.totals(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(new EpidemiologyTotals(5L, 5L));
    when(repository.aggregate(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(List.of(new TerritoryAggregate("3301009", 5L)));

    AnalyticsResult response =
        service.query(
            Geography.MUNICIPALITY,
            new EpidemiologyFilter(null, null, null, null, null, null, null));

    assertEquals(1, response.items().size());
    assertEquals("3301009", response.items().getFirst().code());
    assertEquals(5L, response.items().getFirst().notificationsTotal());
    verify(catalog).forGeography(null, null, "MUNICIPALITY");
  }

  @Test
  void neighborhoodWithDistrictBuildsMappedItems() {
    TerritoryCatalogPort.Entry neighborhood =
        new TerritoryCatalogPort.Entry(
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

    AnalyticsResult response =
        service.query(
            Geography.NEIGHBORHOOD,
            new EpidemiologyFilter(null, null, null, null, null, "3301009", "CG_DIST_SEDE"));

    assertEquals("AVAILABLE", response.coverage().status().name());
    assertEquals(1, response.items().size());
    assertEquals(3L, response.items().getFirst().notificationsTotal());
  }

  @Test
  void sortsTerritoryItemsByCodeBeforeReturningThem() {
    TerritoryCatalogPort.Entry first =
        new TerritoryCatalogPort.Entry("B", "Bairro B", "DISTRICT", "3301009", null, "AVAILABLE");
    TerritoryCatalogPort.Entry second =
        new TerritoryCatalogPort.Entry("A", "Bairro A", "DISTRICT", "3301009", null, "AVAILABLE");
    when(catalog.forGeography("3301009", null, "DISTRICT")).thenReturn(List.of(first, second));
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));

    AnalyticsResult response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter(null, null, null, null, null, "3301009", null));

    assertEquals(List.of("A", "B"), response.items().stream().map(e -> e.code()).toList());
  }

  @Test
  void municipalityWithCodeUsesTheRequestedMunicipalityOnly() {
    TerritoryCatalogPort.Entry municipality =
        new TerritoryCatalogPort.Entry(
            "3302403", "Macaé", "MUNICIPALITY", "3302403", null, "AVAILABLE");
    when(catalog.forGeography("3302403", null, "MUNICIPALITY")).thenReturn(List.of(municipality));
    when(repository.totals(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(new EpidemiologyTotals(2L, 2L));
    when(repository.aggregate(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(List.of(new TerritoryAggregate("3302403", 2L)));

    AnalyticsResult response =
        service.query(
            Geography.MUNICIPALITY,
            new EpidemiologyFilter(null, null, null, null, null, "3302403", null));

    assertEquals(List.of("3302403"), response.items().stream().map(e -> e.code()).toList());
    verify(catalog).forGeography("3302403", null, "MUNICIPALITY");
  }

  @Test
  void buildsNationalAggregatesForStateAndRegion() {
    when(catalog.forGeography(null, null, "STATE")).thenReturn(List.of());
    when(catalog.forGeography(null, null, "REGION")).thenReturn(List.of());
    when(repository.totals(any(), any(), any())).thenReturn(new EpidemiologyTotals(12L, 12L));
    when(repository.aggregate(any(), eq(Geography.STATE), any()))
        .thenReturn(List.of(new TerritoryAggregate("RJ", 7L)));
    when(repository.aggregate(any(), eq(Geography.REGION), any()))
        .thenReturn(List.of(new TerritoryAggregate("SE", 12L)));

    AnalyticsResult state =
        service.query(
            Geography.STATE, new EpidemiologyFilter("DENG", 2024, null, null, null, null, null));
    AnalyticsResult region =
        service.query(
            Geography.REGION, new EpidemiologyFilter("DENG", 2024, null, null, null, null, null));

    assertEquals("RJ", state.items().getFirst().code());
    assertEquals("SE", region.items().getFirst().code());
    assertEquals("Sudeste", region.items().getFirst().name());
  }
}
