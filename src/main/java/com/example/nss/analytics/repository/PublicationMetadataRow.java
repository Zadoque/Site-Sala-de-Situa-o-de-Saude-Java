package com.example.nss.analytics.repository;

import java.time.Instant;

public record PublicationMetadataRow(Instant sourceExtractedAt, Instant addedAt) {}
