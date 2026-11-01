package com.example.nexuscore.config;

import org.bson.BsonNull;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveCallback;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@Component
public class MongoNullOmittingCallback implements BeforeSaveCallback<Object> {

    @Override
    public Object onBeforeSave(Object entity, Document document, String collection) {
        document.entrySet().removeIf(entry -> isNull(entry.getValue()));
        document.replaceAll((key, value) -> omitNulls(value));
        return entity;
    }

    private Object omitNulls(Object value) {
        if (value instanceof Map<?, ?> map) {
            Document document = new Document();
            map.forEach((key, nested) -> {
                if (!isNull(nested)) {
                    document.put(key.toString(), omitNulls(nested));
                }
            });
            return document;
        }
        if (value instanceof Collection<?> values) {
            return values.stream().filter(nested -> !isNull(nested)).map(this::omitNulls).toList();
        }
        if (value instanceof Object[] values) {
            return omitNulls(Arrays.asList(values));
        }
        return value;
    }

    private boolean isNull(Object value) {
        return value == null || value instanceof BsonNull;
    }
}
