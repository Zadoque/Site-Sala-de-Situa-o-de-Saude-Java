package com.example.nss.analytics.web.controller;

import com.example.nss.analytics.application.EpidemiologyService;
import com.example.nss.analytics.application.query.AnalyticsResult;
import com.example.nss.analytics.application.query.MetadataResult;
import com.example.nss.analytics.domain.EpidemiologyFilter;
import com.example.nss.analytics.domain.Geography;
import com.example.nss.analytics.web.response.AnalyticsResponse;
import com.example.nss.analytics.web.response.MetadataResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EpidemiologyController {
  private final EpidemiologyService service;

  @GetMapping("/epidemiology/region")
  public AnalyticsResponse region(@ModelAttribute FilterRequest request) {
    return response(service.query(Geography.REGION, request.toFilter()));
  }

  @GetMapping("/epidemiology/state")
  public AnalyticsResponse state(@ModelAttribute FilterRequest request) {
    return response(service.query(Geography.STATE, request.toFilter()));
  }

  @GetMapping("/epidemiology/municipalities")
  public AnalyticsResponse municipalities(@ModelAttribute FilterRequest request) {
    return response(service.query(Geography.MUNICIPALITY, request.toFilter()));
  }

  @GetMapping("/epidemiology/districts")
  public AnalyticsResponse districts(@ModelAttribute FilterRequest request) {
    return response(service.query(Geography.DISTRICT, request.toFilter()));
  }

  @GetMapping("/epidemiology/neighborhoods")
  public AnalyticsResponse neighborhoods(@ModelAttribute FilterRequest request) {
    return response(service.query(Geography.NEIGHBORHOOD, request.toFilter()));
  }

  @GetMapping("/diseases")
  public DiseaseListResponse diseases() {
    return new DiseaseListResponse(service.diseases());
  }

  @GetMapping("/metadata")
  public MetadataResponse metadata() {
    MetadataResult m = service.metadata();
    return new MetadataResponse(
        m.availableYears(),
        m.availableMonthsByYear(),
        m.supportedGeographies(),
        m.supportedAgeBands(),
        m.supportedSexes());
  }

  private AnalyticsResponse response(AnalyticsResult r) {
    return new AnalyticsResponse(
        r.metric(),
        r.geography(),
        new AnalyticsResponse.FilterResponse(
            r.filters().disease(),
            r.filters().year(),
            r.filters().month(),
            r.filters().sex(),
            r.filters().ageBand(),
            r.filters().municipalityCode(),
            r.filters().districtCode()),
        r.totalNotifications(),
        new AnalyticsResponse.Coverage(
            r.coverage().status(),
            r.coverage().mappedNotificationsTotal(),
            r.coverage().unmappedNotificationsTotal()),
        r.items().stream()
            .map(
                i ->
                    new AnalyticsResponse.TerritoryItem(
                        i.code(), i.name(), i.notificationsTotal(), i.parentDistrictId()))
            .toList());
  }

  public record FilterRequest(
      String disease,
      Integer year,
      Integer month,
      String sex,
      String ageBand,
      String municipalityCode,
      String districtCode) {
    EpidemiologyFilter toFilter() {
      return new EpidemiologyFilter(
          disease, year, month, sex, ageBand, municipalityCode, districtCode);
    }
  }

  public record DiseaseListResponse(List<String> items) {}
}
