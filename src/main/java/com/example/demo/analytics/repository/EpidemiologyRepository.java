package com.example.demo.analytics.repository;

import com.example.demo.analytics.api.EpidemiologyFilter;
import com.example.demo.analytics.api.Geography;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class EpidemiologyRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final String yearColumn;
    private final String monthColumn;

    public EpidemiologyRepository(@Qualifier("analyticsJdbcTemplate") org.springframework.jdbc.core.JdbcTemplate template,
                                  @org.springframework.beans.factory.annotation.Value("${analytics.schema.year-column:year}") String yearColumn,
                                  @org.springframework.beans.factory.annotation.Value("${analytics.schema.month-column:month}") String monthColumn) {
        this.jdbc = new NamedParameterJdbcTemplate(template);
        this.yearColumn = identifier(yearColumn);
        this.monthColumn = identifier(monthColumn);
    }

    public List<Map<String, Object>> aggregate(EpidemiologyFilter f, Geography geography) {
        String territory = switch (geography) {
            case REGION, STATE -> "NULL";
            case MUNICIPALITY -> "f.cd_mun";
            case DISTRICT -> "f.notification_district_id";
            case NEIGHBORHOOD -> "f.notification_neighborhood_id";
        };
        StringBuilder sql = new StringBuilder("SELECT ").append(territory).append(" AS code, SUM(f.cases_total) AS total FROM analytics.fato_casos f WHERE 1=1");
        MapSqlParameterSource p = parameters(f, sql);
        if (geography == Geography.MUNICIPALITY && f.municipalityCode() != null) { sql.append(" AND f.cd_mun = :municipalityCode"); p.addValue("municipalityCode", f.municipalityCode()); }
        if (geography != Geography.MUNICIPALITY && f.municipalityCode() != null) { sql.append(" AND f.cd_mun = :municipalityCode"); p.addValue("municipalityCode", f.municipalityCode()); }
        if (geography == Geography.NEIGHBORHOOD && f.districtCode() != null) { sql.append(" AND f.notification_district_id = :districtCode"); p.addValue("districtCode", f.districtCode()); }
        sql.append(" AND ").append(territory).append(" IS NOT NULL GROUP BY ").append(territory);
        return jdbc.queryForList(sql.toString(), p);
    }

    public Map<String, Object> totals(EpidemiologyFilter f, Geography geography) {
        String mapped = switch (geography) { case REGION, STATE -> "FALSE"; case MUNICIPALITY -> "f.cd_mun IS NOT NULL"; case DISTRICT -> "f.notification_district_id IS NOT NULL"; case NEIGHBORHOOD -> "f.notification_neighborhood_id IS NOT NULL"; };
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(f.cases_total),0) AS total, COALESCE(SUM(CASE WHEN ").append(mapped).append(" THEN f.cases_total ELSE 0 END),0) AS mapped FROM analytics.fato_casos f WHERE 1=1");
        MapSqlParameterSource p = parameters(f, sql);
        if (f.municipalityCode() != null) { sql.append(" AND f.cd_mun = :municipalityCode"); p.addValue("municipalityCode", f.municipalityCode()); }
        if (geography == Geography.NEIGHBORHOOD && f.districtCode() != null) { sql.append(" AND f.notification_district_id = :districtCode"); p.addValue("districtCode", f.districtCode()); }
        return jdbc.queryForMap(sql.toString(), p);
    }

    public List<Map<String, Object>> diseases() { return jdbc.queryForList("SELECT codigo AS code, nome AS name FROM analytics.dim_doenca ORDER BY codigo", Map.of()); }
    public List<Map<String, Object>> metadata() { return jdbc.queryForList("SELECT " + yearColumn + " AS year, " + monthColumn + " AS month FROM analytics.fato_casos GROUP BY " + yearColumn + ", " + monthColumn + " ORDER BY " + yearColumn + ", " + monthColumn, Map.of()); }

    private MapSqlParameterSource parameters(EpidemiologyFilter f, StringBuilder sql) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (f.disease() != null) { sql.append(" AND f.disease_codigo = :disease"); p.addValue("disease", f.disease()); }
        if (f.year() != null) { sql.append(" AND f.").append(yearColumn).append(" = :year"); p.addValue("year", f.year()); }
        if (f.month() != null) { sql.append(" AND f.").append(monthColumn).append(" = :month"); p.addValue("month", f.month()); }
        sql.append(" AND f.cd_sexo IN (:sexes)"); p.addValue("sexes", f.sex() == null ? List.of("M", "F", "I") : List.of(f.sex()));
        if (f.ageBand() != null) { sql.append(" AND f.age_band = :ageBand"); p.addValue("ageBand", f.ageBand()); }
        return p;
    }
    private static String identifier(String value) { if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Identificador SQL inválido"); return value; }
}
