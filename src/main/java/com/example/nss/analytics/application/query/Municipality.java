package com.example.nss.analytics.application.query;

public record Municipality(String code, String name, String ufCode, String ufName) {
  public Municipality {
    if (code == null || !code.matches("\\d{7}")) {
      throw new IllegalArgumentException("Código IBGE municipal deve ter sete dígitos");
    }
    if (ufCode == null || !ufCode.matches("\\d{2}")) {
      throw new IllegalArgumentException("Código IBGE da UF deve ter dois dígitos");
    }
    if (!code.startsWith(ufCode)) {
      throw new IllegalArgumentException("Município não pertence à UF informada");
    }
  }
}
