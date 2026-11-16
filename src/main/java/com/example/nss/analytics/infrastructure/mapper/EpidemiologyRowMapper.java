package com.example.nss.analytics.infrastructure.mapper;

import com.example.nss.analytics.infrastructure.repository.DiseaseRow;
import com.example.nss.analytics.infrastructure.repository.MetadataRow;
import com.example.nss.analytics.infrastructure.repository.MunicipalityOnlyScopeRow;
import com.example.nss.analytics.infrastructure.repository.TerritoryAggregate;
import org.springframework.jdbc.core.RowMapper;

public final class EpidemiologyRowMapper {
  private EpidemiologyRowMapper() {}

  public static RowMapper<TerritoryAggregate> territoryAggregate() {
    return (resultSet, rowNumber) ->
        new TerritoryAggregate(resultSet.getString("code"), resultSet.getLong("total"));
  }

  public static RowMapper<DiseaseRow> disease() {
    return (resultSet, rowNumber) ->
        new DiseaseRow(resultSet.getString("code"), resultSet.getString("name"));
  }

  public static RowMapper<MetadataRow> metadata() {
    return (resultSet, rowNumber) ->
        new MetadataRow(resultSet.getInt("year"), resultSet.getInt("month"));
  }

  public static RowMapper<MunicipalityOnlyScopeRow> municipalityOnlyScope() {
    return (resultSet, rowNumber) ->
        new MunicipalityOnlyScopeRow(resultSet.getString("disease"), resultSet.getInt("year"));
  }
}
