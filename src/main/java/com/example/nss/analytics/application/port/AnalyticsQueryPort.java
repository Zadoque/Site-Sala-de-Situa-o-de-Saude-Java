package com.example.nss.analytics.application.port;

import com.example.nss.analytics.application.query.CasoResumo;
import java.util.List;

@FunctionalInterface
public interface AnalyticsQueryPort {
  List<CasoResumo> buscarCasos(Integer ano, Integer mes);
}
