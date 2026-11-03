package com.example.nss.analytics.application.port;

import com.example.nss.analytics.application.query.CasoResumo;
import java.util.List;

public interface AnalyticsQueryPort {
  List<CasoResumo> buscarCasos(Integer ano, Integer mes);
}
