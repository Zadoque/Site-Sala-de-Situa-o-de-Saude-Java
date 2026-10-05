package com.example.demo.analytics.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class TerritoryCatalog {
    private final Map<String, Entry> entries;
    private final Map<String, String> municipalities = Map.of(
            "3301009", "Campos dos Goytacazes",
            "3302403", "Macaé",
            "3302205", "Itaperuna",
            "3305000", "São João da Barra"
    );

    public TerritoryCatalog() {
        this.entries = load();
    }

    public List<Entry> forGeography(String municipalityCode, String districtCode, String geography) {
        if ("DISTRICT".equals(geography)) return entries.values().stream().filter(e -> "DISTRICT".equals(e.type()) && Objects.equals(municipalityCode, e.municipalityCode())).toList();
        if ("NEIGHBORHOOD".equals(geography)) return entries.values().stream().filter(e -> "NEIGHBORHOOD_OR_LOCALITY".equals(e.type()) && Objects.equals(municipalityCode, e.municipalityCode()) && Objects.equals(districtCode, e.parentDistrictId())).toList();
        if ("MUNICIPALITY".equals(geography)) return municipalities.entrySet().stream().filter(e -> municipalityCode == null || municipalityCode.equals(e.getKey())).map(e -> new Entry(e.getKey(), e.getValue(), "MUNICIPALITY", e.getKey(), null, "AVAILABLE")).toList();
        return List.of();
    }

    public Optional<Entry> find(String id) { return Optional.ofNullable(entries.get(id)); }

    private Map<String, Entry> load() {
        Map<String, Entry> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("territories/campos.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null;) {
                String[] p = line.split("\\t", -1);
                if (p.length >= 6) result.put(p[0], new Entry(p[0], p[1], p[2], p[3], p[4].isBlank() ? null : p[4], p[5]));
            }
        } catch (IOException e) { throw new IllegalStateException("Catálogo territorial não disponível", e); }
        return Map.copyOf(result);
    }

    public record Entry(String territoryId, String name, String type, String municipalityCode, String parentDistrictId, String geometryStatus) {}
}
