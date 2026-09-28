package com.example.nexuscore;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class NexUsCoreApplicationTests {

    @Autowired
    private MongoProperties mongoProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void loadsMongoPropertiesFromBootFourNamespace() {
        assertEquals("mongodb://localhost:27017/nexus_core_test", mongoProperties.getUri());
        assertEquals("nexus_core_test", mongoProperties.getDatabase());
    }

}
