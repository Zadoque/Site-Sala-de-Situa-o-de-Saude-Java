package com.example.nss.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.nss.analytics.service.TerritoryCatalog;
import org.junit.jupiter.api.Test;

class TerritoryCatalogTest {
  private final TerritoryCatalog catalog = new TerritoryCatalog();

  @Test
  void listsDistrictsAndNeighborhoodsForCampos() {
    var districts = catalog.forGeography("3301009", null, "DISTRICT");
    var neighborhoods = catalog.forGeography("3301009", "CG_DIST_SEDE", "NEIGHBORHOOD");

    assertFalse(districts.isEmpty());
    assertFalse(neighborhoods.isEmpty());
    assertEquals("3301009", districts.getFirst().municipalityCode());
    assertEquals("CG_DIST_SEDE", neighborhoods.getFirst().parentDistrictId());
  }

  @Test
  void listsAllMunicipalitiesAndSupportsLookup() {
    var municipalities = catalog.forGeography(null, null, "MUNICIPALITY");

    assertEquals(4, municipalities.size());
    assertTrue(catalog.find("CG_DIST_SEDE").isPresent());
    assertFalse(catalog.find("does-not-exist").isPresent());
  }

  @Test
  void filtersOneMunicipalityAndReturnsEmptyForUnknownGeography() {
    var municipalities = catalog.forGeography("3302403", null, "MUNICIPALITY");

    assertEquals(1, municipalities.size());
    assertEquals("Macaé", municipalities.getFirst().name());
    assertTrue(catalog.forGeography(null, null, "STATE").isEmpty());
  }

  @Test
  void excludesDistrictsAndNeighborhoodsFromOtherParents() {
    assertTrue(catalog.forGeography("3302403", null, "DISTRICT").isEmpty());
    assertTrue(catalog.forGeography("3301009", "unknown", "NEIGHBORHOOD").isEmpty());
  }
}
