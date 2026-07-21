package com.appmcore.mapapp.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.appmcore.mapapp.dto.SymbolMessage;

/**
 * Fans live map symbols out to browsers over Server-Sent Events. The Kafka
 * consumer calls {@link #broadcast} for each message; each connected browser
 * holds one {@link SseEmitter} registered via {@link #subscribe}.
 */
@Service
public class SymbolStreamService {

    private static final Logger log = LoggerFactory.getLogger(SymbolStreamService.class);

    /** Emitters do not time out on their own; browsers reconnect as needed. */
    private static final long NO_TIMEOUT = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Register a new browser subscription.
     *
     * @return the emitter to return from the streaming endpoint (never {@code null})
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        emitters.add(emitter);
        log.debug("Symbol stream subscriber added; {} active", emitters.size());
        return emitter;
    }

    /**
     * Push a symbol to every connected browser, dropping any that have gone away.
     *
     * @param symbol the symbol to send
     */
    public void broadcast(SymbolMessage symbol) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("symbol").data(symbol));
            } catch (IOException | IllegalStateException ex) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    /** Number of currently connected subscribers. */
    public int subscriberCount() {
        return emitters.size();
    }
}
