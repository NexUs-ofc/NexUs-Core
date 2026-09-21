package com.example.nexuscore.categorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GpcCategoryMappingsTest {

    @Test
    void loadsEveryOfficialFoodBrick() {
        Map<String, FoodCategory> mappings = GpcCategoryMappings.load();

        assertEquals(879, mappings.size());
        assertEquals(FoodCategory.SUGARS_AND_SWEETENERS, mappings.get("10000043"));
        assertEquals(FoodCategory.BEVERAGES, mappings.get("10008042"));
        assertEquals(FoodCategory.FROZEN, mappings.get("10000307"));
        assertEquals(FoodCategory.OTHER, mappings.get("10008449"));
    }

    @Test
    void rejectsInvalidHeader() {
        assertThrows(IllegalStateException.class,
                () -> parse("code,category\n10000043,SUGARS_AND_SWEETENERS"));
    }

    @Test
    void rejectsInvalidBrickCode() {
        assertThrows(IllegalStateException.class,
                () -> parse("gpcBrickCode,category\n123,SUGARS_AND_SWEETENERS"));
    }

    @Test
    void rejectsUnknownCategory() {
        assertThrows(IllegalStateException.class,
                () -> parse("gpcBrickCode,category\n10000043,UNKNOWN"));
    }

    @Test
    void rejectsDuplicateBrickCode() {
        assertThrows(IllegalStateException.class,
                () -> parse("gpcBrickCode,category\n10000043,SUGARS_AND_SWEETENERS\n"
                        + "10000043,OTHER"));
    }

    private Map<String, FoodCategory> parse(String content) {
        return GpcCategoryMappings.parse(new StringReader(content));
    }
}
