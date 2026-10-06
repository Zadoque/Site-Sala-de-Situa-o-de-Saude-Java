package com.example.demo.analytics;

import com.example.demo.analytics.api.EpidemiologyFilter;
import com.example.demo.analytics.api.Geography;
import com.example.demo.analytics.repository.EpidemiologyRepository;
import com.example.demo.analytics.service.TerritoryCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EpidemiologyRepositoryTest {
    @Test
    void invalidIdsAndStatusesNeverCountAsMapped() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE SCHEMA analytics");
        jdbc.execute("CREATE TABLE analytics.fato_casos (cd_mun VARCHAR, notification_district_id VARCHAR, notification_neighborhood_id VARCHAR, notification_territory_status VARCHAR, cases_total INT, cd_sexo VARCHAR)");
        var catalog = new TerritoryCatalog();
        String district = "CG_DIST_SEDE";
        String neighborhood = catalog.forGeography("3301009", district, "NEIGHBORHOOD").getFirst().territoryId();
        Object[][] rows = {
            {district, neighborhood, "NOTIFICATION_NEIGHBORHOOD", 3},
            {district, null, "NOTIFICATION_DISTRICT_ONLY", 2},
            {"NaN", "NaN", "UNMAPPED_NOTIFICATION_UNIT", 7},
            {"UNKNOWN", null, "NOTIFICATION_DISTRICT_ONLY", 11},
            {district, neighborhood, "UNMAPPED_NOTIFICATION_UNIT", 13},
            {null, null, "UNMAPPED_NOTIFICATION_UNIT", 17}
        };
        for (Object[] row : rows) jdbc.update("INSERT INTO analytics.fato_casos VALUES ('3301009', ?, ?, ?, ?, 'F')", row);
        var repository = new EpidemiologyRepository(jdbc, "ano", "mes", catalog);
        var filter = new EpidemiologyFilter(null, null, null, null, null, "3301009", null);
        var totals = repository.totals(filter, Geography.DISTRICT);
        assertEquals(53L, ((Number) totals.get("total")).longValue());
        assertEquals(5L, ((Number) totals.get("mapped")).longValue());
        assertEquals(1, repository.aggregate(filter, Geography.DISTRICT).size());
        var neighborhoods = new EpidemiologyFilter(null, null, null, null, null, "3301009", district);
        assertEquals(3L, ((Number) repository.totals(neighborhoods, Geography.NEIGHBORHOOD).get("mapped")).longValue());
    }
}
