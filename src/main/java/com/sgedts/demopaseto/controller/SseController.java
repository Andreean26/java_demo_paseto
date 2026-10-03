package com.sgedts.demopaseto.controller;

import com.sgedts.demopaseto.service.DemoStateService;
import com.sgedts.demopaseto.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SseController {

    private final SseService sseService;
    private final DemoStateService stateService;

    public SseController(SseService sseService, DemoStateService stateService) {
        this.sseService = sseService;
        this.stateService = stateService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events() {
        SseEmitter emitter = sseService.createEmitter();

        // Send initial snapshot
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mode", stateService.getMode().toLowerString());
        snapshot.put("events", stateService.getEvents());
        sseService.sendToEmitter(emitter, "snapshot", snapshot);

        return emitter;
    }
}
