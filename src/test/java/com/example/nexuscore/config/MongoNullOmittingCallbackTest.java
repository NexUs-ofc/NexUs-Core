package com.example.nexuscore.config;

import com.example.nexuscore.model.ActivityRecord;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeIngredient;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
import org.bson.BsonNull;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MongoNullOmittingCallbackTest {

    private final MongoNullOmittingCallback callback = new MongoNullOmittingCallback();
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

    @ParameterizedTest
    @MethodSource("entities")
    void omitsNullsFromEveryCurrentCollectionAndEmbeddedObjects(Object entity, String collection) {
        Document document = new Document();
        converter.write(entity, document);

        assertThat(callback.onBeforeSave(entity, document, collection)).isSameAs(entity);

        assertNoNulls(document);
    }

    @Test
    void removesNullFieldsAndElementsRecursivelyWithoutChangingOtherValues() {
        byte[] binary = {1, 2};
        Document document = new Document("missing", null).append("bsonNull", BsonNull.VALUE)
                .append("false", false).append("zero", 0).append("empty", "").append("binary", binary)
                .append("nested", new Document("missing", null).append("kept", "value"))
                .append("list", Arrays.asList(null, BsonNull.VALUE, new Document("missing", null).append("kept", 1)))
                .append("array", new Object[] {null, 2, new Document("missing", null)});

        callback.onBeforeSave(new Object(), document, "any_collection");

        assertNoNulls(document);
        assertThat(document).doesNotContainKeys("missing", "bsonNull");
        assertThat(document.get("false")).isEqualTo(false);
        assertThat(document.get("zero")).isEqualTo(0);
        assertThat(document.get("empty")).isEqualTo("");
        assertThat(document.get("binary")).isSameAs(binary);
        assertThat(document.getList("list", Document.class)).containsExactly(new Document("kept", 1));
        assertThat(document.get("array")).isEqualTo(List.of(2, new Document()));
    }

    private static Stream<Arguments> entities() {
        ShoppingList list = new ShoppingList(12, "Compras", null);
        list.getArrayList().add(new ShoppingListItem(null, "Arroz", 2.0, null, false));

        Recipe recipe = BeanUtils.instantiateClass(Recipe.class);
        RecipeIngredient ingredient = BeanUtils.instantiateClass(RecipeIngredient.class);
        ReflectionTestUtils.setField(ingredient, "foodId", 7);
        ReflectionTestUtils.setField(ingredient, "mandatory", false);
        ReflectionTestUtils.setField(ingredient, "possibleSubstitutes", Arrays.asList(null, 8, null));
        ReflectionTestUtils.setField(recipe, "ingredients", Arrays.asList(ingredient, null));

        return Stream.of(Arguments.of(list, "shopping_list"), Arguments.of(recipe, "recipes"),
                Arguments.of(new ActivityRecord(12, "stock_item_added", null), "records"));
    }

    private void assertNoNulls(Object value) {
        assertThat(value).isNotNull().isNotInstanceOf(BsonNull.class);
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(this::assertNoNulls);
        } else if (value instanceof Collection<?> values) {
            values.forEach(this::assertNoNulls);
        }
    }
}
