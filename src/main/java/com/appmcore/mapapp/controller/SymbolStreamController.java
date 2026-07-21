package com.appmcore.mapapp.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.appmcore.mapapp.service.SymbolStreamService;

/**
 * Streams live map symbols (sourced from Kafka) to the browser over
 * Server-Sent Events.
 */
@RestController
@RequestMapping("/api/v1/map/symbols")
public class SymbolStreamController {

    private final SymbolStreamService symbolStreamService;

    public SymbolStreamController(SymbolStreamService symbolStreamService) {
        this.symbolStreamService = symbolStreamService;
    }

    /**
     * Subscribe to the live symbol stream. The browser connects with an
     * {@code EventSource} and receives a {@code symbol} event per update.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream() {
        return ResponseEntity.ok(symbolStreamService.subscribe());
    }
}
