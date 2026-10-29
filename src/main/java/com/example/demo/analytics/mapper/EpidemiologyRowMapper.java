package com.example.demo.analytics.mapper;

import com.example.demo.analytics.repository.DiseaseRow;
import com.example.demo.analytics.repository.MetadataRow;
import com.example.demo.analytics.repository.TerritoryAggregate;
import org.springframework.jdbc.core.RowMapper;

public final class EpidemiologyRowMapper {
    private EpidemiologyRowMapper() {
    }

    public static RowMapper<TerritoryAggregate> territoryAggregate() {
        return (resultSet, rowNumber) -> new TerritoryAggregate(
                resultSet.getString("code"),
                resultSet.getLong("total")
        );
    }

    public static RowMapper<DiseaseRow> disease() {
        return (resultSet, rowNumber) -> new DiseaseRow(
                resultSet.getString("code"),
                resultSet.getString("name")
        );
    }

    public static RowMapper<MetadataRow> metadata() {
        return (resultSet, rowNumber) -> new MetadataRow(
                resultSet.getInt("year"),
                resultSet.getInt("month")
        );
    }
}
