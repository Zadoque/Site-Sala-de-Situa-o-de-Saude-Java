package com.example.nss.analytics.dto.mapper;

import com.example.nss.analytics.dto.response.AnalyticsResponse;
import com.example.nss.analytics.dto.response.CasoResumoResponse;
import com.example.nss.analytics.dto.response.MetadataResponse;
import com.example.nss.analytics.entity.AnalyticsResult;
import com.example.nss.analytics.entity.MetadataResult;
import com.example.nss.analytics.repository.CasoResumo;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {
  public AnalyticsResponse toResponse(AnalyticsResult value) {
    return new AnalyticsResponse(
        value.metric(),
        value.geography(),
        new AnalyticsResponse.FilterResponse(
            value.filters().disease(),
            value.filters().year(),
            value.filters().month(),
            value.filters().sex(),
            value.filters().ageBand(),
            value.filters().municipalityCode(),
            value.filters().districtCode(),
            value.filters().ufCode(),
            value.filters().regionCode()),
        value.totalNotifications(),
        new AnalyticsResponse.Coverage(
            value.coverage().status(),
            value.coverage().mappedNotificationsTotal(),
            value.coverage().unmappedNotificationsTotal(),
            value.coverage().territorialLevel(),
            value.coverage().reason()),
        value.items().stream()
            .map(
                item ->
                    new AnalyticsResponse.TerritoryItem(
                        item.code(),
                        item.name(),
                        item.notificationsTotal(),
                        item.parentDistrictId()))
            .toList(),
        value.sourceExtractedAt(),
        value.addedAt());
  }

  public MetadataResponse toResponse(MetadataResult value) {
    return new MetadataResponse(
        value.availableYears(),
        value.availableMonthsByYear(),
        value.supportedGeographies(),
        value.supportedAgeBands(),
        value.supportedSexes(),
        value.municipalityOnlyScopes().stream()
            .map(scope -> new MetadataResponse.MunicipalityOnlyScope(scope.disease(), scope.year()))
            .toList());
  }

  public CasoResumoResponse toResponse(CasoResumo value) {
    return new CasoResumoResponse(
        value.codigoDoenca(),
        value.ano(),
        value.mes(),
        value.codigoMunicipio(),
        value.codigoUnidade(),
        value.classificacao(),
        value.evolucao(),
        value.sexo(),
        value.semanaNotificacao(),
        value.anoNascimento(),
        value.totalCasos());
  }
}
