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
import java.security.spec.ECGenParameterSpec;

@Configuration
public class TokenConfig {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${demo.jwt.secret}")
    private String jwtSecret;

    @Value("${demo.paseto.local-key}")
    private String pasetoLocalKeyStr;

    private byte[] cachedKeyBytes;
    private KeyPair cachedV4KeyPair;
    private KeyPair cachedV3KeyPair;

    /**
     * HMAC-SHA256 key for JWT, derived via SHA-256 from the secret string.
     * Returns javax.crypto.SecretKey (compatible with jjwt).
     */
    @Bean(name = "jwtSecretKey")
    public javax.crypto.SecretKey jwtSecretKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 32-byte hash SHA-256 string secret.
     */
    @Bean(name = "pasetoLocalKeyBytes")
    public synchronized byte[] pasetoLocalKeyBytes() {
        if (cachedKeyBytes == null) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                cachedKeyBytes = digest.digest(pasetoLocalKeyStr.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
        return cachedKeyBytes;
    }

    /**
     * SecretKey for PASETO v4.local.
     */
    @Bean(name = "pasetoV4SecretKey")
    public SecretKey pasetoV4SecretKey() {
        return new SecretKey(pasetoLocalKeyBytes(), Version.V4);
    }

    /**
     * SecretKey for PASETO v3.local.
     */
    @Bean(name = "pasetoV3SecretKey")
    public SecretKey pasetoV3SecretKey() {
        return new SecretKey(pasetoLocalKeyBytes(), Version.V3);
    }

    /**
     * Ed25519 KeyPair for PASETO v4.public.
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
                throw new RuntimeException("Ed25519 key generation failed", e);
            }
        }
        return cachedV4KeyPair;
    }

    @Bean(name = "pasetoV4PrivateKey")
    public PrivateKey pasetoV4PrivateKey() {
        return new PrivateKey(pasetoV4KeyPair().getPrivate(), Version.V4);
    }

    @Bean(name = "pasetoV4PublicKey")
    public PublicKey pasetoV4PublicKey() {
        return new PublicKey(pasetoV4KeyPair().getPublic(), Version.V4);
    }

    /**
     * EC P-384 KeyPair (secp384r1) for PASETO v3.public.
     */
    @Bean(name = "pasetoV3KeyPair")
    public synchronized KeyPair pasetoV3KeyPair() {
        if (cachedV3KeyPair == null) {
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
                kpg.initialize(new ECGenParameterSpec("secp384r1"));
                cachedV3KeyPair = kpg.generateKeyPair();
            } catch (Exception e) {
                try {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                    kpg.initialize(new ECGenParameterSpec("secp384r1"));
                    cachedV3KeyPair = kpg.generateKeyPair();
                } catch (Exception ex) {
                    throw new RuntimeException("EC P-384 key generation failed", ex);
                }
            }
        }
        return cachedV3KeyPair;
    }

    @Bean(name = "pasetoV3PrivateKey")
    public PrivateKey pasetoV3PrivateKey() {
        return new PrivateKey(pasetoV3KeyPair().getPrivate(), Version.V3);
    }

    @Bean(name = "pasetoV3PublicKey")
    public PublicKey pasetoV3PublicKey() {
        return new PublicKey(pasetoV3KeyPair().getPublic(), Version.V3);
    }
}
