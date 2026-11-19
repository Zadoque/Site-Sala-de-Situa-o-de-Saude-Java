package com.example.nss.analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.nss.analytics.entity.AnalyticsResult;
import com.example.nss.analytics.entity.EpidemiologyFilter;
import com.example.nss.analytics.entity.Geography;
import com.example.nss.analytics.repository.AnalyticsResponseCache;
import com.example.nss.analytics.repository.DiseaseRow;
import com.example.nss.analytics.repository.EpidemiologyRepository;
import com.example.nss.analytics.repository.EpidemiologyTotals;
import com.example.nss.analytics.repository.MetadataCatalogRow;
import com.example.nss.analytics.repository.TerritoryAggregate;
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
  @Mock AnalyticsResponseCache cache;
  @InjectMocks EpidemiologyService service;

  @Test
  void totalOfficialIsNotCalculatedFromVisibleItems() {
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
  void fullyUnmappedMunicipalitySignalsMunicipalityOnlySource() {
    when(repository.totals(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(new EpidemiologyTotals(7305L, 0L));
    when(repository.notificationUnitTotals(any())).thenReturn(new EpidemiologyTotals(7305L, 0L));
    when(repository.aggregate(any(), eq(Geography.MUNICIPALITY), any())).thenReturn(List.of());

    AnalyticsResult response =
        service.query(
            Geography.MUNICIPALITY,
            new EpidemiologyFilter("SIFA", 2025, null, null, null, "3304557", null));

    assertEquals("MUNICIPALITY_ONLY", response.coverage().territorialLevel());
    assertEquals("NOTIFICATION_UNIT_NOT_PROVIDED_BY_SOURCE", response.coverage().reason());
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
  void cacheHitUsesAllFilterDimensionsAndAvoidsRepositoryAccess() {
    var filter = new EpidemiologyFilter("DENG", 2026, 3, "M", "20_39", "3301009", "CG_DIST");
    var cached = service.unavailable(Geography.DISTRICT, filter);
    service.setCache(cache);
    when(cache.get("DISTRICT|DENG|2026|3|M|20_39|3301009|CG_DIST|null|null")).thenReturn(cached);

    assertSame(cached, service.query(Geography.DISTRICT, filter));
    verify(cache).get("DISTRICT|DENG|2026|3|M|20_39|3301009|CG_DIST|null|null");
    verifyNoInteractions(repository, catalog);
  }

  @Test
  void nationalIntramunicipalDrilldownUsesTheNationalCatalog() {
    when(catalog.forGeography("3302205", null, "DISTRICT"))
        .thenReturn(
            List.of(
                new TerritoryCatalog.Entry(
                    "IBGE_DIST_330220505", "Itaperuna", "DISTRICT", "3302205", null, "AVAILABLE")));
    when(repository.totals(any(), eq(Geography.DISTRICT), any()))
        .thenReturn(new EpidemiologyTotals(0L, 0L));
    when(repository.aggregate(any(), eq(Geography.DISTRICT), any())).thenReturn(List.of());

    AnalyticsResult response =
        service.query(
            Geography.DISTRICT,
            new EpidemiologyFilter("DENG", null, null, null, null, "3302205", null));

    assertEquals(0L, response.totalNotifications());
    assertEquals("AVAILABLE", response.coverage().status().name());
    assertEquals(1, response.items().size());
    verify(repository).totals(any(), eq(Geography.DISTRICT), any());
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
    when(repository.metadataCatalog())
        .thenReturn(
            List.of(
                new MetadataCatalogRow("DENG", 2024, List.of(1, 2), false),
                new MetadataCatalogRow("DENG", 2026, List.of(3), false),
                new MetadataCatalogRow("SIFA", 2025, List.of(1), true)));
    when(repository.diseases())
        .thenReturn(List.of(new DiseaseRow("DENG", "Dengue"), new DiseaseRow("COVID", "Covid-19")));

    assertEquals(List.of("DENG", "COVID"), service.diseases());
    assertEquals(List.of(2024, 2025, 2026), service.metadata().availableYears());
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

    assertEquals(2, response.items().size());
    assertEquals("3301009", response.items().getFirst().code());
    assertEquals(5L, response.items().getFirst().notificationsTotal());
    assertEquals(0L, response.items().get(1).notificationsTotal());
    verify(catalog, times(2)).forGeography(null, null, "MUNICIPALITY");
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
    TerritoryCatalog.Entry first =
        new TerritoryCatalog.Entry("B", "Bairro B", "DISTRICT", "3301009", null, "AVAILABLE");
    TerritoryCatalog.Entry second =
        new TerritoryCatalog.Entry("A", "Bairro A", "DISTRICT", "3301009", null, "AVAILABLE");
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
    TerritoryCatalog.Entry municipality =
        new TerritoryCatalog.Entry(
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
  void municipalityWithUfKeepsOnlyMunicipalitiesFromThatUf() {
    when(catalog.forGeography(null, null, "MUNICIPALITY"))
        .thenReturn(
            List.of(
                new TerritoryCatalog.Entry(
                    "3302403", "Macaé", "MUNICIPALITY", "3302403", null, "AVAILABLE"),
                new TerritoryCatalog.Entry(
                    "3106200", "Belo Horizonte", "MUNICIPALITY", "3106200", null, "AVAILABLE")));
    when(repository.totals(any(), eq(Geography.MUNICIPALITY), any()))
        .thenReturn(new EpidemiologyTotals(1L, 1L));
    when(repository.aggregate(any(), eq(Geography.MUNICIPALITY), any())).thenReturn(List.of());

    var response =
        service.query(
            Geography.MUNICIPALITY,
            new EpidemiologyFilter(null, null, null, null, null, null, null, "33", null));

    assertEquals(
        List.of("3302403"),
        response.items().stream().map(AnalyticsResult.TerritoryItem::code).toList());
  }

  @Test
  void regionFilterOnlyReturnsStatesFromTheRequestedRegion() {
    when(repository.totals(any(), any(), any())).thenReturn(new EpidemiologyTotals(0L, 0L));
    when(repository.aggregate(any(), any(), any())).thenReturn(List.of());

    var response =
        service.query(
            Geography.STATE,
            new EpidemiologyFilter(null, null, null, null, null, null, null, null, "SE"));

    assertFalse(response.items().isEmpty());
    assertTrue(
        response.items().stream()
            .allMatch(
                item ->
                    item.code().equals("31")
                        || item.code().equals("32")
                        || item.code().equals("33")
                        || item.code().equals("35")));
  }

  @Test
  void regionAndStateQueriesExposeNationalAreaItems() {
    when(repository.totals(any(), any(), any())).thenReturn(new EpidemiologyTotals(15L, 15L));
    when(repository.aggregate(any(), any(), any())).thenReturn(List.of());

    var filter = new EpidemiologyFilter("DENG", 2026, 1, null, null, null, null, null, null);
    var regions = service.query(Geography.REGION, filter);
    assertEquals(
        List.of("CO", "N", "NE", "S", "SE"),
        regions.items().stream().map(AnalyticsResult.TerritoryItem::code).sorted().toList());

    for (String region : List.of("N", "NE", "SE", "S", "CO")) {
      var states =
          service.query(
              Geography.STATE,
              new EpidemiologyFilter("DENG", 2026, 1, null, null, null, null, null, region));
      assertFalse(states.items().isEmpty());
    }
  }
}
