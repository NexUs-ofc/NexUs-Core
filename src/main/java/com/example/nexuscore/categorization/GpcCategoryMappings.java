package com.example.nexuscore.categorization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class GpcCategoryMappings {

    private static final String RESOURCE =
            "/categorization/gpc-brick-categories.csv";
    private static final String HEADER = "gpcBrickCode,category";
    private static final Pattern BRICK_CODE = Pattern.compile("\\d{8}");

    private GpcCategoryMappings() {
    }

    static Map<String, FoodCategory> load() {
        InputStream source = GpcCategoryMappings.class.getResourceAsStream(RESOURCE);
        if (source == null) {
            throw new IllegalStateException("Mapeamento GPC não encontrado");
        }
        return parse(new InputStreamReader(source, StandardCharsets.UTF_8));
    }

    static Map<String, FoodCategory> parse(Reader source) {
        try (BufferedReader reader = new BufferedReader(source)) {
            if (!HEADER.equals(reader.readLine())) {
                throw new IllegalStateException("Cabeçalho inválido no mapeamento GPC");
            }

            Map<String, FoodCategory> mappings = new HashMap<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                addMapping(mappings, line, lineNumber);
            }

            if (mappings.isEmpty()) {
                throw new IllegalStateException("Mapeamento GPC vazio");
            }
            return Map.copyOf(mappings);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao carregar mapeamento GPC", exception);
        }
    }

    private static void addMapping(Map<String, FoodCategory> mappings,
                                   String line,
                                   int lineNumber) {
        String[] fields = line.split(",", -1);
        if (fields.length != 2) {
            throw invalidLine(lineNumber);
        }

        String brickCode = fields[0].trim();
        if (!BRICK_CODE.matcher(brickCode).matches()) {
            throw invalidLine(lineNumber);
        }

        FoodCategory category;
        try {
            category = FoodCategory.valueOf(fields[1].trim());
        } catch (IllegalArgumentException exception) {
            throw invalidLine(lineNumber);
        }

        if (mappings.putIfAbsent(brickCode, category) != null) {
            throw new IllegalStateException(
                    "Código GPC duplicado na linha " + lineNumber);
        }
    }

    private static IllegalStateException invalidLine(int lineNumber) {
        return new IllegalStateException(
                "Linha inválida no mapeamento GPC: " + lineNumber);
    }
}
