package com.example.nss.analytics.application;

import com.example.nss.analytics.application.port.AnalyticsQueryPort;
import com.example.nss.analytics.application.query.CasoResumo;
import com.example.nss.analytics.web.response.CasoResumoResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private final AnalyticsQueryPort repository;

  public List<CasoResumoResponse> buscarCasos(Integer ano, Integer mes) {
    return repository.buscarCasos(ano, mes).stream().map(this::toResponse).toList();
  }

  private CasoResumoResponse toResponse(CasoResumo caso) {
    return new CasoResumoResponse(
        caso.codigoDoenca(),
        caso.ano(),
        caso.mes(),
        caso.codigoMunicipio(),
        caso.codigoUnidade(),
        caso.classificacao(),
        caso.evolucao(),
        caso.sexo(),
        caso.semanaNotificacao(),
        caso.anoNascimento(),
        caso.totalCasos());
  }
}
