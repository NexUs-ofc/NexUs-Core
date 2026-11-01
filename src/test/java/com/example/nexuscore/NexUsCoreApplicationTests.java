package com.example.nexuscore;

import com.example.nexuscore.geo.GeocodingProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class NexUsCoreApplicationTests {

    @Autowired
    private MongoProperties mongoProperties;

    @Autowired
    private HikariDataSource dataSource;

    @Autowired
    private GeocodingProperties geocodingProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void loadsMongoPropertiesFromBootFourNamespace() {
        assertEquals("mongodb://localhost:27017/nexus_core_test", mongoProperties.getUri());
        assertEquals("nexus_core_test", mongoProperties.getDatabase());
    }

    @Test
    void limitsPostgresConnectionPoolSize() {
        assertEquals(5, dataSource.getMaximumPoolSize());
    }

    @Test
    void loadsDistanceMatrixApiKeyFromConfiguration() {
        assertEquals("test-geocoding-key", geocodingProperties.apiKey());
    }

}
