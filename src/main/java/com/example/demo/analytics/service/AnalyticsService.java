package com.example.demo.analytics.service;

import com.example.demo.analytics.dto.CasoResumo;
import com.example.demo.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final AnalyticsRepository repository;

    public List<CasoResumo> buscarCasos(Integer ano, Integer mes) {
        return repository.buscarCasos(ano, mes);
    }
}
