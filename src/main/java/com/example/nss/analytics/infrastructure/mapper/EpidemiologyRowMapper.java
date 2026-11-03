package com.example.nss.analytics.infrastructure.mapper;

import com.example.nss.analytics.application.query.Disease;
import com.example.nss.analytics.application.query.MetadataEntry;
import com.example.nss.analytics.application.query.TerritoryAggregate;
import org.springframework.jdbc.core.RowMapper;

public final class EpidemiologyRowMapper {
  private EpidemiologyRowMapper() {}

  public static RowMapper<TerritoryAggregate> territoryAggregate() {
    return (resultSet, rowNumber) ->
        new TerritoryAggregate(resultSet.getString("code"), resultSet.getLong("total"));
  }

  public static RowMapper<Disease> disease() {
    return (resultSet, rowNumber) ->
        new Disease(resultSet.getString("code"), resultSet.getString("name"));
  }

  public static RowMapper<MetadataEntry> metadata() {
    return (resultSet, rowNumber) ->
        new MetadataEntry(resultSet.getInt("year"), resultSet.getInt("month"));
  }
}
