package com.example.nss.analytics.service;

import com.example.nss.analytics.dto.CasoResumo;
import com.example.nss.analytics.repository.AnalyticsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private final AnalyticsRepository repository;

  public List<CasoResumo> buscarCasos(Integer ano, Integer mes) {
    return repository.buscarCasos(ano, mes);
  }
}
