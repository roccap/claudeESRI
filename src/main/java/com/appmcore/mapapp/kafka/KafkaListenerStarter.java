package com.appmcore.mapapp.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

/**
 * Starts the Kafka listeners after the application is ready, rather than during
 * context refresh. This keeps a missing or unreachable broker from aborting
 * startup: if the listeners fail to start, the app still runs (serving the map
 * and REST API) — just without the live symbol stream until a restart.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerStarter {

    private static final Logger log = LoggerFactory.getLogger(KafkaListenerStarter.class);

    private final KafkaListenerEndpointRegistry registry;

    public KafkaListenerStarter(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListeners() {
        try {
            registry.start();
            log.info("Kafka listeners started");
        } catch (Exception ex) {
            log.warn("Kafka listeners failed to start (broker unavailable?); the app "
                + "will run without the live symbol stream until restarted: {}", ex.getMessage());
        }
    }
}
