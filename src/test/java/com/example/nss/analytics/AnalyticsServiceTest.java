package com.example.nss.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nss.analytics.dto.CasoResumo;
import com.example.nss.analytics.repository.AnalyticsRepository;
import com.example.nss.analytics.service.AnalyticsService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
  @Test
  void delegatesCaseSearchToTheRepository() {
    AnalyticsRepository repository = mock(AnalyticsRepository.class);
    AnalyticsService service = new AnalyticsService(repository);
    List<CasoResumo> expected = List.of();
    when(repository.buscarCasos(2024, 3)).thenReturn(expected);

    assertEquals(expected, service.buscarCasos(2024, 3));
    verify(repository).buscarCasos(2024, 3);
  }
}
