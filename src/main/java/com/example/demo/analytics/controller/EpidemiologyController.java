package com.example.demo.analytics.controller;

import com.example.demo.analytics.api.*;
import com.example.demo.analytics.repository.EpidemiologyRepository;
import com.example.demo.analytics.service.EpidemiologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EpidemiologyController {
    private final EpidemiologyService service;
    private final EpidemiologyRepository repository;

    @GetMapping("/epidemiology/municipalities")
    public AnalyticsResponse municipalities(@ModelAttribute FilterRequest request) { return service.query(Geography.MUNICIPALITY, request.toFilter()); }
    @GetMapping("/epidemiology/districts")
    public AnalyticsResponse districts(@ModelAttribute FilterRequest request) { return service.query(Geography.DISTRICT, request.toFilter()); }
    @GetMapping("/epidemiology/neighborhoods")
    public AnalyticsResponse neighborhoods(@ModelAttribute FilterRequest request) { return service.query(Geography.NEIGHBORHOOD, request.toFilter()); }

    @GetMapping("/diseases")
    public List<DiseaseResponse> diseases() { return repository.diseases().stream().map(row -> new DiseaseResponse(String.valueOf(row.get("code")), String.valueOf(row.get("name")), true)).toList(); }

    @GetMapping("/metadata")
    public MetadataResponse metadata() {
        Map<Integer, List<Integer>> grouped = repository.metadata().stream().collect(Collectors.groupingBy(r -> ((Number) r.get("year")).intValue(), LinkedHashMap::new, Collectors.mapping(r -> ((Number) r.get("month")).intValue(), Collectors.toList())));
        return new MetadataResponse(new ArrayList<>(grouped.keySet()), grouped.entrySet().stream().collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue, (a,b) -> a, LinkedHashMap::new)),
                List.of("MUNICIPALITY", "DISTRICT", "NEIGHBORHOOD"), new ArrayList<>(EpidemiologyFilter.AGE_BANDS), List.of("M", "F", "ALL"));
    }

    public record FilterRequest(String disease, Integer year, Integer month, String sex, String ageBand, String municipalityCode, String districtCode) {
        EpidemiologyFilter toFilter() { return new EpidemiologyFilter(disease, year, month, sex, ageBand, municipalityCode, districtCode); }
    }
}
