package com.example.demo.analytics;

import com.example.demo.analytics.api.EpidemiologyFilter;
import com.example.demo.exception.InvalidFilterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EpidemiologyFilterTest {
    @Test void omittedSexMeansAllIsRepresentable() { assertNull(new EpidemiologyFilter("DENG", null, null, null, null, null, null).sex()); }
    @Test void acceptsAgeBandAndPeriodOmission() { assertDoesNotThrow(() -> new EpidemiologyFilter("DENG", null, null, "F", "20_39", "3301009", null)); }
    @Test void rejectsInvalidMonth() { assertThrows(InvalidFilterException.class, () -> new EpidemiologyFilter("DENG", 2026, 13, null, null, null, null)); }
    @Test void rejectsInvalidSex() { assertThrows(InvalidFilterException.class, () -> new EpidemiologyFilter("DENG", 2026, 1, "I", null, null, null)); }
    @Test void rejectsInvalidMunicipalityCode() { assertThrows(InvalidFilterException.class, () -> new EpidemiologyFilter("DENG", 2026, 1, null, null, "x", null)); }
}
