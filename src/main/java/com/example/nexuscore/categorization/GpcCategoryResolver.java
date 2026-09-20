package com.example.nexuscore.categorization;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GpcCategoryResolver {

    public static final String FALLBACK_CATEGORY = "Outros";

    private static final Map<String, String> BRICK_TO_CATEGORY = Map.of(
            "10000043", "Açúcar e Adoçantes"
    );

    public String resolve(String gpcBrickCode) {
        if (gpcBrickCode == null || gpcBrickCode.isBlank()) {
            return FALLBACK_CATEGORY;
        }
        return BRICK_TO_CATEGORY.getOrDefault(gpcBrickCode, FALLBACK_CATEGORY);
    }
}
