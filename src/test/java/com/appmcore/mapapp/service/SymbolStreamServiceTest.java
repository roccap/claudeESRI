package com.appmcore.mapapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.appmcore.mapapp.dto.SymbolMessage;

class SymbolStreamServiceTest {

    private final SymbolStreamService service = new SymbolStreamService();

    private static SymbolMessage sampleSymbol() {
        return new SymbolMessage(
            "a1", new BigDecimal("51.51"), new BigDecimal("-0.12"), "triangle", "#1E88E5");
    }

    @Test
    void subscribeRegistersAnEmitter() {
        assertThat(service.subscriberCount()).isZero();

        SseEmitter emitter = service.subscribe();

        assertThat(emitter).isNotNull();
        assertThat(service.subscriberCount()).isEqualTo(1);
    }

    @Test
    void broadcastDeliversToSubscribersWithoutError() {
        service.subscribe();

        assertThatCode(() -> service.broadcast(sampleSymbol())).doesNotThrowAnyException();
        assertThat(service.subscriberCount()).isEqualTo(1);
    }

    @Test
    void broadcastWithNoSubscribersIsANoop() {
        assertThatCode(() -> service.broadcast(sampleSymbol())).doesNotThrowAnyException();
        assertThat(service.subscriberCount()).isZero();
    }
}
