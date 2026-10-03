package com.sgedts.demopaseto.controller;

import com.sgedts.demopaseto.model.DemoMode;
import com.sgedts.demopaseto.service.DemoStateService;
import com.sgedts.demopaseto.service.JwtService;
import com.sgedts.demopaseto.service.PasetoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final DemoStateService stateService;
    private final JwtService jwtService;
    private final PasetoService pasetoService;

    public AuthController(DemoStateService stateService,
                          JwtService jwtService,
                          PasetoService pasetoService) {
        this.stateService = stateService;
        this.jwtService = jwtService;
        this.pasetoService = pasetoService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        if (name.length() > 60) name = name.substring(0, 60);
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Nama wajib diisi"));
        }

        String pasetoFormat = String.valueOf(request.getOrDefault("pasetoFormat", "v4.local"));
        DemoMode mode = stateService.getMode();

        String token;
        try {
            if (mode == DemoMode.JWT) {
                token = jwtService.makeJwt(name);
            } else {
                token = pasetoService.makeToken(name, pasetoFormat);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("mode", mode.toLowerString());
        body.put("name", name);
        body.put("role", "USER");
        if (mode == DemoMode.PASETO) {
            body.put("pasetoFormat", pasetoFormat);
        }
        body.put("token", token);
        return ResponseEntity.ok(body);
    }
}
