package com.example.nexuscore.model;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListMongoMappingTest {

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
    void omitsDateFromDocumentWhenNotProvided() {
        Document document = new Document();

        converter.write(new ShoppingList(12, "Compras", null), document);

        assertThat(document).doesNotContainKeys("date", "event_id");
        assertThat(converter.read(ShoppingList.class, document).getDate()).isNull();
    }

    @Test
    void writesAndReadsProvidedDate() {
        LocalDate date = LocalDate.of(2026, 12, 10);
        Document document = new Document();

        converter.write(new ShoppingList(12, "Compras", date), document);

        assertThat(document.get("date")).isInstanceOf(Date.class);
        assertThat(converter.read(ShoppingList.class, document).getDate()).isEqualTo(date);
    }

    @Test
    void readsListWithoutDate() {
        Document document = new Document("_id", "list-id").append("household_id", 12)
                .append("title", "Compras");

        ShoppingList list = converter.read(ShoppingList.class, document);
        Document saved = new Document();
        converter.write(list, saved);

        assertThat(list.getId()).isEqualTo("list-id");
        assertThat(list.getDate()).isNull();
        assertThat(saved).doesNotContainKeys("date", "event_id");
    }

    @Test
    void omitsDateWhenItsValueIsNull() {
        ShoppingList list = new ShoppingList(12, "Compras", LocalDate.of(2026, 12, 10));
        list.setDate(null);
        Document document = new Document();

        converter.write(list, document);

        assertThat(document).doesNotContainKey("date");
    }
}
