package com.example.nss.analytics.application;

import com.example.nss.analytics.application.port.AnalyticsQueryPort;
import com.example.nss.analytics.application.query.CaseResult;
import com.example.nss.analytics.application.query.CasoResumo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private final AnalyticsQueryPort repository;

  public List<CaseResult> buscarCasos(Integer ano, Integer mes) {
    return repository.buscarCasos(ano, mes).stream().map(this::toResult).toList();
  }

  private CaseResult toResult(CasoResumo caso) {
    return new CaseResult(
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
