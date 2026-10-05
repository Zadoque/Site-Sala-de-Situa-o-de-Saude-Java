package com.example.demo.analytics;

import com.example.demo.analytics.api.AnalyticsResponse;
import com.example.demo.analytics.api.EpidemiologyFilter;
import com.example.demo.analytics.api.Geography;
import com.example.demo.analytics.repository.EpidemiologyRepository;
import com.example.demo.analytics.service.EpidemiologyService;
import com.example.demo.analytics.service.TerritoryCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpidemiologyServiceTest {
    @Mock EpidemiologyRepository repository;
    @Mock TerritoryCatalog catalog;
    @InjectMocks EpidemiologyService service;

    @Test
    void totalOfficialIsNotCalculatedFromVisibleItems() {
        when(repository.totals(any(), eq(Geography.DISTRICT))).thenReturn(Map.of("total", 10L, "mapped", 7L));
        when(repository.aggregate(any(), eq(Geography.DISTRICT))).thenReturn(List.of(Map.of("code", "CG_DIST_SEDE", "total", 7L)));

        AnalyticsResponse response = service.query(Geography.DISTRICT,
                new EpidemiologyFilter("DENG", 2026, 3, null, null, "3301009", null));

        assertEquals(10L, response.totalNotifications());
        assertEquals(7L, response.coverage().mappedNotificationsTotal());
        assertEquals(3L, response.coverage().unmappedNotificationsTotal());
        assertEquals("PARTIAL", response.coverage().status().name());
    }

    @Test
    void neighborhoodWithoutDistrictIsUnavailable() {
        AnalyticsResponse response = service.query(Geography.NEIGHBORHOOD,
                new EpidemiologyFilter("DENG", null, null, null, null, "3301009", null));

        assertNull(response.totalNotifications());
        assertEquals("UNAVAILABLE", response.coverage().status().name());
        assertTrue(response.items().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void nonCamposIntramunicipalDrilldownIsUnavailable() {
        AnalyticsResponse response = service.query(Geography.DISTRICT,
                new EpidemiologyFilter("DENG", null, null, null, null, "3302205", null));

        assertNull(response.totalNotifications());
        assertEquals("UNAVAILABLE", response.coverage().status().name());
        verifyNoInteractions(repository);
    }

    @Test
    void availableZeroIsDifferentFromUnavailable() {
        when(repository.totals(any(), eq(Geography.DISTRICT))).thenReturn(Map.of("total", 0L, "mapped", 0L));
        when(repository.aggregate(any(), eq(Geography.DISTRICT))).thenReturn(List.of());

        AnalyticsResponse response = service.query(Geography.DISTRICT,
                new EpidemiologyFilter("DENG", 2026, null, null, "20_39", "3301009", null));

        assertEquals(0L, response.totalNotifications());
        assertEquals("AVAILABLE", response.coverage().status().name());
    }
}
