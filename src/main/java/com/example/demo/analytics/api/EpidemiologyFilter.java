package com.example.demo.analytics.api;

import com.example.demo.exception.InvalidFilterException;

import java.util.List;
import java.util.Set;

public record EpidemiologyFilter(String disease, Integer year, Integer month, String sex,
                                 String ageBand, String municipalityCode, String districtCode) {
    public static final List<String> AGE_BANDS = List.of("LT1", "01_04", "05_09", "10_14", "15_19", "20_39", "40_59", "60_64", "65_69", "70_74", "75_79", "80_PLUS");

    public EpidemiologyFilter {
        if (year != null && year <= 0) throw new InvalidFilterException("Parâmetro year deve ser inteiro positivo");
        if (month != null && (month < 1 || month > 12)) throw new InvalidFilterException("Parâmetro month deve estar entre 1 e 12");
        if (sex != null && !Set.of("M", "F").contains(sex)) throw new InvalidFilterException("Parâmetro sex deve ser M ou F");
        if (ageBand != null && !AGE_BANDS.contains(ageBand)) throw new InvalidFilterException("Parâmetro ageBand inválido");
        if (disease != null && disease.isBlank()) throw new InvalidFilterException("Parâmetro disease inválido");
        if (municipalityCode != null && !municipalityCode.matches("\\d{7}")) throw new InvalidFilterException("Parâmetro municipalityCode inválido");
        if (districtCode != null && districtCode.isBlank()) throw new InvalidFilterException("Parâmetro districtCode inválido");
    }
}
