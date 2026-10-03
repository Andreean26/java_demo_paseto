package com.sgedts.demopaseto.controller;

import com.sgedts.demopaseto.model.DemoEvent;
import com.sgedts.demopaseto.model.DemoMode;
import com.sgedts.demopaseto.service.DemoStateService;
import com.sgedts.demopaseto.service.SseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StateController {

    private final DemoStateService stateService;
    private final SseService sseService;

    public StateController(DemoStateService stateService, SseService sseService) {
        this.stateService = stateService;
        this.sseService = sseService;
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> getState() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("mode", stateService.getMode().toLowerString());
        body.put("events", stateService.getEvents());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/mode")
    public ResponseEntity<Map<String, Object>> setMode(@RequestBody Map<String, Object> request) {
        String modeStr = String.valueOf(request.getOrDefault("mode", ""));
        DemoMode newMode;
        try {
            newMode = DemoMode.fromString(modeStr);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = Map.of("ok", false, "error", "Mode harus jwt atau paseto.");
            return ResponseEntity.badRequest().body(err);
        }

        stateService.setMode(newMode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Mode diganti ke " + newMode.name());
        payload.put("mode", newMode.toLowerString());
        DemoEvent event = stateService.addEvent("mode", payload);

        // Broadcast the event
        sseService.broadcast("mode", event);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("mode", newMode.toLowerString());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        stateService.clearEvents();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("mode", stateService.getMode().toLowerString());
        snapshot.put("events", List.of());
        sseService.broadcast("snapshot", snapshot);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("mode", stateService.getMode().toLowerString());
        body.put("events", List.of());
        return ResponseEntity.ok(body);
    }
}
