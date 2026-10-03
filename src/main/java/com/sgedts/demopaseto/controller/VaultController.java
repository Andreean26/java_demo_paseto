package com.sgedts.demopaseto.controller;

import com.sgedts.demopaseto.model.DemoMode;
import com.sgedts.demopaseto.service.DemoStateService;
import com.sgedts.demopaseto.service.JwtService;
import com.sgedts.demopaseto.service.PasetoService;
import com.sgedts.demopaseto.service.SseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class VaultController {

    private final DemoStateService stateService;
    private final JwtService jwtService;
    private final PasetoService pasetoService;
    private final SseService sseService;

    public VaultController(DemoStateService stateService,
                           JwtService jwtService,
                           PasetoService pasetoService,
                           SseService sseService) {
        this.stateService = stateService;
        this.jwtService = jwtService;
        this.pasetoService = pasetoService;
        this.sseService = sseService;
    }

    @PostMapping("/access")
    public ResponseEntity<Map<String, Object>> access(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract Bearer token
        String token;
        try {
            token = extractBearer(authHeader);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }

        DemoMode mode = stateService.getMode();

        if (mode == DemoMode.JWT) {
            return handleJwt(token);
        } else {
            return handlePaseto(token);
        }
    }

    private ResponseEntity<Map<String, Object>> handleJwt(String token) {
        try {
            JwtService.JwtVerifyResult result = jwtService.verifyJwtVulnerable(token);
            Map<String, Object> claims = result.claims();
            String name = String.valueOf(claims.getOrDefault("name", "Anonim"));
            if (name.length() > 60) name = name.substring(0, 60);
            String role = String.valueOf(claims.getOrDefault("role", "UNKNOWN")).toUpperCase();

            if ("ADMIN".equals(role)) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("title", "SISTEM DIRETAS OLEH: " + name);
                payload.put("name", name);
                payload.put("role", role);
                payload.put("warning", result.warning() != null ? result.warning() : "JWT valid dengan role ADMIN");
                var event = stateService.addEvent("hacked", payload);
                sseService.broadcast("hacked", event);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("ok", true);
                body.put("status", "HACKED");
                body.put("message", "Vault terbuka. Presenter akan melihat nama " + name + ".");
                body.put("claims", claims);
                body.put("warning", result.warning());
                return ResponseEntity.ok(body);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", false);
            body.put("status", "DENIED");
            body.put("message", "Token valid, tapi role masih USER. Ubah payload kalau berani.");
            body.put("claims", claims);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "status", "INVALID", "error", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> handlePaseto(String token) {
        try {
            Map<String, Object> claims = pasetoService.verifyToken(token);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", false);
            body.put("status", "DENIED");
            body.put("message", "Token secure valid, tapi role tetap USER. Brankas tetap terkunci.");
            body.put("claims", claims);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        } catch (Exception e) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", "Percobaan token secure diblokir");
            payload.put("detail", e.getMessage());
            var event = stateService.addEvent("blocked", payload);
            sseService.broadcast("blocked", event);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "ok", false,
                            "status", "BLOCKED",
                            "error", "Token secure rusak atau tidak autentik. Akses ditolak."
                    ));
        }
    }

    private String extractBearer(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new IllegalArgumentException("Header Authorization Bearer tidak ditemukan");
        }
        String trimmed = authHeader.trim();
        if (!trimmed.toLowerCase().startsWith("bearer ")) {
            throw new IllegalArgumentException("Header Authorization Bearer tidak ditemukan");
        }
        String token = trimmed.substring(7).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Token Bearer kosong");
        }
        return token;
    }
}
