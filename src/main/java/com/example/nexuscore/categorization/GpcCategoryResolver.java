package com.example.nexuscore.categorization;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GpcCategoryResolver {

    public static final String FALLBACK_CATEGORY = FoodCategory.OTHER.getDisplayName();

    private static final Map<String, FoodCategory> BRICK_TO_CATEGORY =
            GpcCategoryMappings.load();

    public String resolve(String gpcBrickCode) {
        if (gpcBrickCode == null || gpcBrickCode.isBlank()) {
            return FALLBACK_CATEGORY;
        }
        return BRICK_TO_CATEGORY.getOrDefault(gpcBrickCode.trim(), FoodCategory.OTHER)
                .getDisplayName();
    }
}
