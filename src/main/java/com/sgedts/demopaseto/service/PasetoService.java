package com.sgedts.demopaseto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * PASETO token service using paseto4j library (v4 and v3 support).
 */
@Service
public class PasetoService {

    private final SecretKey v4SecretKey;
    private final PrivateKey v4PrivateKey;
    private final PublicKey v4PublicKey;

    private final SecretKey v3SecretKey;
    private final PrivateKey v3PrivateKey;
    private final PublicKey v3PublicKey;

    private final ObjectMapper objectMapper;

    public PasetoService(@Qualifier("pasetoV4SecretKey") SecretKey v4SecretKey,
                         @Qualifier("pasetoV4PrivateKey") PrivateKey v4PrivateKey,
                         @Qualifier("pasetoV4PublicKey") PublicKey v4PublicKey,
                         @Qualifier("pasetoV3SecretKey") SecretKey v3SecretKey,
                         @Qualifier("pasetoV3PrivateKey") PrivateKey v3PrivateKey,
                         @Qualifier("pasetoV3PublicKey") PublicKey v3PublicKey,
                         ObjectMapper objectMapper) {
        this.v4SecretKey = v4SecretKey;
        this.v4PrivateKey = v4PrivateKey;
        this.v4PublicKey = v4PublicKey;
        this.v3SecretKey = v3SecretKey;
        this.v3PrivateKey = v3PrivateKey;
        this.v3PublicKey = v3PublicKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a PASETO token.
     * format: "v4.local", "v4.public", "v3.local", "v3.public"
     */
    public String makeToken(String name, String format) {
        long iatSeconds = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name);
        claims.put("role", "USER");
        claims.put("iat", iatSeconds);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(claims);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize claims to JSON", e);
        }

        try {
            if ("v4.public".equalsIgnoreCase(format)) {
                return org.paseto4j.version4.Paseto.sign(v4PrivateKey, jsonPayload, "");
            } else if ("v3.local".equalsIgnoreCase(format)) {
                return org.paseto4j.version3.Paseto.encrypt(v3SecretKey, jsonPayload, "");
            } else if ("v3.public".equalsIgnoreCase(format)) {
                return org.paseto4j.version3.Paseto.sign(v3PrivateKey, jsonPayload, "");
            } else {
                // Default to v4.local
                return org.paseto4j.version4.Paseto.encrypt(v4SecretKey, jsonPayload, "");
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal membuat token PASETO: " + e.getMessage(), e);
        }
    }

    /**
     * Verify a PASETO token (v4.local, v4.public, v3.local, v3.public).
     */
    public Map<String, Object> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token tidak boleh kosong");
        }

        String jsonPayload;
        try {
            if (token.startsWith("v4.local.")) {
                jsonPayload = org.paseto4j.version4.Paseto.decrypt(v4SecretKey, token, "");
            } else if (token.startsWith("v4.public.")) {
                jsonPayload = org.paseto4j.version4.Paseto.parse(v4PublicKey, token, "");
            } else if (token.startsWith("v3.local.")) {
                jsonPayload = org.paseto4j.version3.Paseto.decrypt(v3SecretKey, token, "");
            } else if (token.startsWith("v3.public.")) {
                jsonPayload = org.paseto4j.version3.Paseto.parse(v3PublicKey, token, "");
            } else {
                throw new IllegalArgumentException(
                        "Token secure harus berformat v4.local, v4.public, v3.local, atau v3.public. " +
                        "Token yang diterima: " + token.substring(0, Math.min(20, token.length())) + "...");
            }

            return objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Verifikasi token gagal atau token rusak: " + e.getMessage(), e);
        }
    }
}
