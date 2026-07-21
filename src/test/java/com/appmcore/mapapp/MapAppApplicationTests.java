package com.appmcore.mapapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Kafka is disabled here so the context loads without a running broker.
@SpringBootTest
@TestPropertySource(properties = "app.kafka.enabled=false")
class MapAppApplicationTests {

    @Test
    void contextLoads() {
    }
}
