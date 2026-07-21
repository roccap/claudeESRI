package com.appmcore.mapapp.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.appmcore.mapapp.dto.SymbolMessage;
import com.appmcore.mapapp.service.SymbolStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consumes the map-symbols Kafka topic and fans each symbol out to browsers via
 * {@link SymbolStreamService}. Enabled by default; disable with
 * {@code app.kafka.enabled=false} (e.g. in tests or when no broker is present).
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SymbolStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(SymbolStreamConsumer.class);

    private final SymbolStreamService symbolStreamService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SymbolStreamConsumer(SymbolStreamService symbolStreamService) {
        this.symbolStreamService = symbolStreamService;
    }

    @KafkaListener(
        topics = "${app.kafka.symbols-topic}",
        groupId = "${spring.kafka.consumer.group-id:map-app}")
    public void onMessage(String payload) {
        try {
            SymbolMessage symbol = objectMapper.readValue(payload, SymbolMessage.class);
            if (symbol.id() == null || symbol.latitude() == null || symbol.longitude() == null) {
                log.warn("Ignoring symbol message missing id/latitude/longitude: {}", payload);
                return;
            }
            symbolStreamService.broadcast(symbol);
            log.debug("Broadcast symbol {}", symbol.id());
        } catch (Exception ex) {
            log.warn("Failed to parse symbol message: {}", payload, ex);
        }
    }
}
