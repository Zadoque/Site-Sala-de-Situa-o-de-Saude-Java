package com.example.nss.analytics.repository;

import com.example.nss.analytics.repository.DiseaseRow;
import com.example.nss.analytics.repository.MetadataCatalogRow;
import com.example.nss.analytics.repository.TerritoryAggregate;
import org.springframework.jdbc.core.RowMapper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

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

  public static RowMapper<MetadataCatalogRow> metadataCatalog() {
    return (resultSet, rowNumber) -> {
      Object sqlArray = resultSet.getArray("available_months").getArray();
      List<Integer> months = new ArrayList<>();
      for (int i = 0; i < Array.getLength(sqlArray); i++) {
        months.add(((Number) Array.get(sqlArray, i)).intValue());
      }
      return new MetadataCatalogRow(
          resultSet.getString("disease"),
          resultSet.getInt("year"),
          List.copyOf(months),
          resultSet.getBoolean("municipality_only"));
    };
  }
}
