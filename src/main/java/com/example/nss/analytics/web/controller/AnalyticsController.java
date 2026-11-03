package com.example.nss.analytics.web.controller;

import com.example.nss.analytics.application.AnalyticsService;
import com.example.nss.analytics.application.query.CaseResult;
import com.example.nss.analytics.web.response.CasoResumoResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("casos")
  public List<CasoResumoResponse> buscarCasos(
      @RequestParam Integer ano, @RequestParam Integer mes) {
    return analyticsService.buscarCasos(ano, mes).stream().map(this::toResponse).toList();
  }

  private CasoResumoResponse toResponse(CaseResult c) {
    return new CasoResumoResponse(
        c.codigoDoenca(),
        c.ano(),
        c.mes(),
        c.codigoMunicipio(),
        c.codigoUnidade(),
        c.classificacao(),
        c.evolucao(),
        c.sexo(),
        c.semanaNotificacao(),
        c.anoNascimento(),
        c.totalCasos());
  }
}
