package com.example.demo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailNormalizerTest {
    @Test
    void trimsAndLowercasesEmail() {
        assertEquals("ana@example.com", EmailNormalizer.normalize("  Ana@Example.COM "));
    }

    @Test
    void preservesNull() {
        assertNull(EmailNormalizer.normalize(null));
    }
}
