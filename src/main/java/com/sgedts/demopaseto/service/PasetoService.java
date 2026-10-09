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
 * Service untuk pembuatan dan verifikasi token PASETO Version 4 (paseto4j).
 * <p>
 * Mendukung 2 mode resmi:
 * <ul>
 *   <li><b>v4.local</b>: Enkripsi simetris AEAD (XChaCha20 + BLAKE2b-MAC, 256-bit key).</li>
 *   <li><b>v4.public</b>: Tanda tangan digital asimetris (Ed25519).</li>
 * </ul>
 */
@Service
public class PasetoService {

    private final SecretKey v4SecretKey;
    private final PrivateKey v4PrivateKey;
    private final PublicKey v4PublicKey;
    private final ObjectMapper objectMapper;

    public PasetoService(@Qualifier("pasetoV4SecretKey") SecretKey v4SecretKey,
                         @Qualifier("pasetoV4PrivateKey") PrivateKey v4PrivateKey,
                         @Qualifier("pasetoV4PublicKey") PublicKey v4PublicKey,
                         ObjectMapper objectMapper) {
        this.v4SecretKey = v4SecretKey;
        this.v4PrivateKey = v4PrivateKey;
        this.v4PublicKey = v4PublicKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Membuat token PASETO v4 ("v4.local" atau "v4.public").
     *
     * @param name   Nama peserta/user
     * @param format "v4.local" (default) atau "v4.public"
     * @return String token PASETO resmi
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
            throw new RuntimeException("Gagal serialisasi claims ke JSON", e);
        }

        try {
            if ("v4.public".equalsIgnoreCase(format)) {
                // Asimetris Ed25519 (v4.public)
                return org.paseto4j.version4.Paseto.sign(v4PrivateKey, jsonPayload, "");
            } else {
                // Simetris AEAD XChaCha20 + BLAKE2b-MAC (v4.local)
                return org.paseto4j.version4.Paseto.encrypt(v4SecretKey, jsonPayload, "");
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal menerbitkan token PASETO v4: " + e.getMessage(), e);
        }
    }

    /**
     * Memverifikasi token PASETO v4 dan mengembalikan claims payload.
     *
     * @param token String token ("v4.local..." atau "v4.public...")
     * @return Map berisi claims payload
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
            } else {
                throw new IllegalArgumentException(
                        "Token secure harus berformat v4.local atau v4.public. " +
                        "Diterima: " + token.substring(0, Math.min(20, token.length())) + "...");
            }

            return objectMapper.readValue(jsonPayload, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Verifikasi token gagal atau token rusak: " + e.getMessage(), e);
        }
    }
}
