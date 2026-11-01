package com.example.nexuscore.model;

import com.example.nexuscore.mapper.RecipeMapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeMongoMappingTest {

    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() throws Exception {
        MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> {});
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
    }

    @Test
    void readsAiIngredientNamesWithoutFoodIds() {
        Document document = new Document("_id", new ObjectId()).append("title", "Frango com arroz")
                .append("serving_size", 2).append("account_id", 12)
                .append("ingredients", List.of(
                        new Document("name", "Peito de frango").append("required_quantity", "500 g")
                                .append("mandatory", true),
                        new Document("name", "Arroz branco").append("required_quantity", "200 g")
                                .append("mandatory", true)));

        Recipe recipe = converter.read(Recipe.class, document);

        assertThat(RecipeMapper.toResponse(recipe, Map.of()).ingredients())
                .extracting(ingredient -> ingredient.foodName())
                .containsExactly("Peito de frango", "Arroz branco");
        assertThat(recipe.getIngredients()).allSatisfy(ingredient -> assertThat(ingredient.getFoodId()).isNull());
        Document saved = new Document();
        converter.write(recipe, saved);
        assertThat(saved.getList("ingredients", Document.class)).extracting(ingredient -> ingredient.getString("name"))
                .containsExactly("Peito de frango", "Arroz branco");
    }
}
