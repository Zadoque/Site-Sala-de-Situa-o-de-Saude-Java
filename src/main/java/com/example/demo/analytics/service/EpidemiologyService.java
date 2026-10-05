package com.example.demo.analytics.service;

import com.example.demo.analytics.api.AnalyticsResponse;
import com.example.demo.analytics.api.CoverageStatus;
import com.example.demo.analytics.api.EpidemiologyFilter;
import com.example.demo.analytics.api.Geography;
import com.example.demo.analytics.repository.EpidemiologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EpidemiologyService {
    private static final String CAMPOS = "3301009";
    private final EpidemiologyRepository repository;
    private final TerritoryCatalog catalog;

    public AnalyticsResponse query(Geography geography, EpidemiologyFilter filter) {
        if (geography != Geography.MUNICIPALITY && filter.municipalityCode() != null && !CAMPOS.equals(filter.municipalityCode())) {
            return response(geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, List.of());
        }
        if (geography == Geography.NEIGHBORHOOD && !"CG_DIST_SEDE".equals(filter.districtCode())) {
            return response(geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, List.of());
        }
        Map<String, Object> totals = repository.totals(filter, geography);
        long total = number(totals.get("total"));
        long mapped = number(totals.get("mapped"));
        List<AnalyticsResponse.TerritoryItem> items = new ArrayList<>();
        Map<String, Long> aggregates = new HashMap<>();
        for (Map<String, Object> row : repository.aggregate(filter, geography)) aggregates.put(String.valueOf(row.get("code")), number(row.get("total")));
        for (TerritoryCatalog.Entry entry : catalog.forGeography(filter.municipalityCode(), filter.districtCode(), geography.name())) {
            items.add(new AnalyticsResponse.TerritoryItem(entry.territoryId(), entry.name(), aggregates.getOrDefault(entry.territoryId(), 0L), entry.parentDistrictId()));
        }
        if (geography == Geography.MUNICIPALITY && filter.municipalityCode() == null) {
            items = new ArrayList<>();
            for (TerritoryCatalog.Entry entry : catalog.forGeography(null, null, geography.name())) items.add(new AnalyticsResponse.TerritoryItem(entry.territoryId(), entry.name(), aggregates.getOrDefault(entry.territoryId(), 0L), null));
        }
        items.sort(Comparator.comparing(AnalyticsResponse.TerritoryItem::code));
        CoverageStatus status = total == 0 || mapped == total ? CoverageStatus.AVAILABLE : CoverageStatus.PARTIAL;
        return response(geography, filter, total, status, mapped, total - mapped, items);
    }

    public AnalyticsResponse unavailable(Geography geography, EpidemiologyFilter filter) {
        return response(geography, filter, null, CoverageStatus.UNAVAILABLE, 0L, 0L, List.of());
    }

    private AnalyticsResponse response(Geography geography, EpidemiologyFilter f, Long total, CoverageStatus status,
                                       long mapped, long unmapped, List<AnalyticsResponse.TerritoryItem> items) {
        return new AnalyticsResponse("notifications", geography,
                new AnalyticsResponse.FilterResponse(f.disease(), f.year(), f.month(), f.sex(), f.ageBand(), f.municipalityCode(), f.districtCode()),
                total, new AnalyticsResponse.Coverage(status, mapped, unmapped), items);
    }
    private long number(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
}
