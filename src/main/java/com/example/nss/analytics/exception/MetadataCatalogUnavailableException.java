package com.example.nss.analytics.exception;

public class MetadataCatalogUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MetadataCatalogUnavailableException(String message) {
    super(message);
  }
}
