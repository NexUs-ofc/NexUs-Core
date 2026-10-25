package com.example.nexuscore.converter;

import com.example.nexuscore.model.RecipeDifficulty;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import java.text.Normalizer;
import java.util.Locale;

public final class RecipeDifficultyConverter
        implements PropertyValueConverter<RecipeDifficulty, String, MongoConversionContext> {

    @Override
    public RecipeDifficulty read(String value, MongoConversionContext context) {
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EASY", "FACIL" -> RecipeDifficulty.EASY;
            case "MEDIUM", "MEDIA", "MEDIO" -> RecipeDifficulty.MEDIUM;
            case "HARD", "DIFICIL" -> RecipeDifficulty.HARD;
            default -> throw new IllegalArgumentException("Dificuldade de receita invalida: " + value);
        };
    }

    @Override
    public String write(RecipeDifficulty value, MongoConversionContext context) {
        return value.name();
    }
}
