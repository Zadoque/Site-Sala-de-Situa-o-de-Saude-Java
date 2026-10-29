package com.example.nss.analytics.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.analytics.dto.CasoResumo;
import com.example.nss.analytics.service.AnalyticsService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsControllerTest {
  @Test
  void delegatesCaseEndpointParametersToTheService() {
    AnalyticsService service = mock(AnalyticsService.class);
    AnalyticsController controller = new AnalyticsController(service);
    List<CasoResumo> expected = List.of();
    when(service.buscarCasos(2024, 3)).thenReturn(expected);

    assertEquals(expected, controller.buscarCasos(2024, 3));
    verify(service).buscarCasos(2024, 3);
  }
}
