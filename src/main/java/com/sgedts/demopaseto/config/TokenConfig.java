package com.sgedts.demopaseto.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.paseto4j.commons.Version;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * Konfigurasi kunci kriptografi untuk JWT dan PASETO v4:
 * <ul>
 *   <li><b>JWT (HS256)</b>: Shared secret 256-bit (HMAC-SHA256).</li>
 *   <li><b>PASETO (v4.local)</b>: SecretKey 256-bit (XChaCha20 + BLAKE2b-MAC AEAD).</li>
 *   <li><b>PASETO (v4.public) &amp; JWT (EdDSA)</b>: KeyPair asimetris Ed25519.</li>
 * </ul>
 */
@Configuration
public class TokenConfig {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${demo.jwt.secret:live-demo-jwt-secret-key-32-chars-long}")
    private String jwtSecret;

    @Value("${demo.paseto.local-key:live-demo-paseto-local-key-32-bytes}")
    private String pasetoLocalKeyStr;

    private byte[] cachedKeyBytes;
    private KeyPair cachedV4KeyPair;

    /**
     * Kunci simetris 256-bit untuk JWT HS256.
     */
    @Bean(name = "jwtSecretKey")
    public javax.crypto.SecretKey jwtSecretKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 tidak tersedia", e);
        }
    }

    /**
     * Array 32-byte hasil SHA-256 untuk kunci simetris PASETO v4.local.
     */
    @Bean(name = "pasetoLocalKeyBytes")
    public synchronized byte[] pasetoLocalKeyBytes() {
        if (cachedKeyBytes == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                cachedKeyBytes = digest.digest(pasetoLocalKeyStr.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 tidak tersedia", e);
            }
        }
        return cachedKeyBytes;
    }

    /**
     * SecretKey 256-bit paseto4j untuk enkripsi AEAD v4.local.
     */
    @Bean(name = "pasetoV4SecretKey")
    public SecretKey pasetoV4SecretKey() {
        return new SecretKey(pasetoLocalKeyBytes(), Version.V4);
    }

    /**
     * KeyPair kurva Ed25519 untuk PASETO v4.public dan JWT EdDSA.
     */
    @Bean(name = "pasetoV4KeyPair")
    public synchronized KeyPair pasetoV4KeyPair() {
        if (cachedV4KeyPair == null) {
            try {
                KeyPairGenerator kpg;
                try {
                    kpg = KeyPairGenerator.getInstance("Ed25519", "BC");
                } catch (Exception e) {
                    kpg = KeyPairGenerator.getInstance("Ed25519");
                }
                cachedV4KeyPair = kpg.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("KeyPairGenerator Ed25519 gagal", e);
            }
        }
        return cachedV4KeyPair;
    }

    /**
     * PrivateKey Ed25519 paseto4j untuk penandatanganan token v4.public.
     */
    @Bean(name = "pasetoV4PrivateKey")
    public PrivateKey pasetoV4PrivateKey() {
        return new PrivateKey(pasetoV4KeyPair().getPrivate(), Version.V4);
    }

    /**
     * PublicKey Ed25519 paseto4j untuk verifikasi tanda tangan token v4.public.
     */
    @Bean(name = "pasetoV4PublicKey")
    public PublicKey pasetoV4PublicKey() {
        return new PublicKey(pasetoV4KeyPair().getPublic(), Version.V4);
    }
}
