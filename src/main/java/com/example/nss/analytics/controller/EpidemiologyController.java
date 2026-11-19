package com.example.nss.analytics.controller;

import com.example.nss.analytics.dto.mapper.AnalyticsMapper;
import com.example.nss.analytics.dto.response.AnalyticsResponse;
import com.example.nss.analytics.dto.response.MetadataResponse;
import com.example.nss.analytics.entity.EpidemiologyFilter;
import com.example.nss.analytics.entity.Geography;
import com.example.nss.analytics.service.EpidemiologyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EpidemiologyController {
  private final EpidemiologyService service;
  private final AnalyticsMapper mapper;

  @GetMapping("/epidemiology/region")
  public AnalyticsResponse region(@ModelAttribute FilterRequest request) {
    return mapper.toResponse(service.query(Geography.REGION, request.toFilter()));
  }

  @GetMapping("/epidemiology/state")
  public AnalyticsResponse state(@ModelAttribute FilterRequest request) {
    return mapper.toResponse(service.query(Geography.STATE, request.toFilter()));
  }

  @GetMapping("/epidemiology/municipalities")
  public AnalyticsResponse municipalities(@ModelAttribute FilterRequest request) {
    return mapper.toResponse(service.query(Geography.MUNICIPALITY, request.toFilter()));
  }

  @GetMapping("/epidemiology/districts")
  public AnalyticsResponse districts(@ModelAttribute FilterRequest request) {
    return mapper.toResponse(service.query(Geography.DISTRICT, request.toFilter()));
  }

  @GetMapping("/epidemiology/neighborhoods")
  public AnalyticsResponse neighborhoods(@ModelAttribute FilterRequest request) {
    return mapper.toResponse(service.query(Geography.NEIGHBORHOOD, request.toFilter()));
  }

  @GetMapping("/diseases")
  public DiseaseListResponse diseases() {
    return new DiseaseListResponse(service.diseases());
  }

  @GetMapping("/metadata")
  public MetadataResponse metadata() {
    return mapper.toResponse(service.metadata());
  }

  public record FilterRequest(
      String disease,
      Integer year,
      Integer month,
      String sex,
      String ageBand,
      String municipalityCode,
      String districtCode,
      String ufCode,
      String regionCode) {
    public FilterRequest(
        String disease,
        Integer year,
        Integer month,
        String sex,
        String ageBand,
        String municipalityCode,
        String districtCode) {
      this(disease, year, month, sex, ageBand, municipalityCode, districtCode, null, null);
    }

    EpidemiologyFilter toFilter() {
      return new EpidemiologyFilter(
          disease, year, month, sex, ageBand, municipalityCode, districtCode, ufCode, regionCode);
    }
  }

  public record DiseaseListResponse(List<String> items) {}
}
