package com.sgedts.demopaseto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey jwtSecretKey;
    private final ObjectMapper objectMapper;

    public JwtService(@Qualifier("jwtSecretKey") SecretKey jwtSecretKey,
                      ObjectMapper objectMapper) {
        this.jwtSecretKey = jwtSecretKey;
        this.objectMapper = objectMapper;
    }

    /**
     * Make a JWT signed with HS256.
     */
    public String makeJwt(String name) {
        long iatSeconds = System.currentTimeMillis() / 1000;
        return Jwts.builder()
                .claim("name", name)
                .claim("role", "USER")
                .claim("iat", iatSeconds)
                .issuedAt(new Date())
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Vulnerable JWT verify: accepts alg:none without signature verification.
     * This is INTENTIONALLY insecure for the demo.
     */
    public JwtVerifyResult verifyJwtVulnerable(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("JWT format tidak valid");
        }

        // Decode header without verification
        Map<String, Object> header = decodeBase64UrlJson(parts[0]);
        if (header == null) {
            throw new IllegalArgumentException("JWT header tidak valid");
        }

        String alg = String.valueOf(header.getOrDefault("alg", "")).toLowerCase();

        if (alg.equals("none")) {
            // alg:none attack: accept claims WITHOUT verifying signature
            String payloadPart = parts.length > 1 ? parts[1] : "";
            if (payloadPart.isEmpty()) {
                throw new IllegalArgumentException("JWT payload kosong");
            }
            Map<String, Object> claims = decodeBase64UrlJson(payloadPart);
            if (claims == null) {
                throw new IllegalArgumentException("JWT payload tidak valid");
            }
            return new JwtVerifyResult(claims, "alg:none diterima tanpa verifikasi signature");
        }

        if (!alg.equals("hs256")) {
            throw new IllegalArgumentException("Algoritma JWT tidak didukung: " + header.get("alg"));
        }

        // Normal HS256 verification
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Map<String, Object> claimsMap = new HashMap<>(claims);
        return new JwtVerifyResult(claimsMap, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeBase64UrlJson(String base64Url) {
        try {
            String padded = base64Url;
            int mod = padded.length() % 4;
            if (mod == 2) padded += "==";
            else if (mod == 3) padded += "=";

            byte[] decoded = Base64.getUrlDecoder().decode(padded);
            return objectMapper.readValue(decoded, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public record JwtVerifyResult(Map<String, Object> claims, String warning) {}
}
