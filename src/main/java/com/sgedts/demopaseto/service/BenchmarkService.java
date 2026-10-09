package com.sgedts.demopaseto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BenchmarkService {

    private final javax.crypto.SecretKey jwtSecretKey;
    private final SecretKey pasetoV4SecretKey;
    private final PrivateKey pasetoV4PrivateKey;
    private final PublicKey pasetoV4PublicKey;
    private final java.security.KeyPair pasetoV4KeyPair;
    private final ObjectMapper objectMapper;

    // Payload presets identical to JS original
    private static final Map<String, Map<String, Object>> PAYLOAD_PRESETS = new LinkedHashMap<>();

    static {
        // minimal
        Map<String, Object> minimal = new LinkedHashMap<>();
        minimal.put("sub", "usr_1001");
        minimal.put("role", "USER");
        PAYLOAD_PRESETS.put("minimal", minimal);

        // standard
        Map<String, Object> standard = new LinkedHashMap<>();
        standard.put("sub", "usr_948271029");
        standard.put("name", "Alice W. Johnson");
        standard.put("email", "alice.johnson@example.com");
        standard.put("role", "USER");
        standard.put("department", "Infrastructure & SecOps");
        standard.put("permissions", List.of("read:profile", "write:notes", "access:vault"));
        standard.put("iss", "https://auth.company.local");
        standard.put("aud", "api.company.local");
        PAYLOAD_PRESETS.put("standard", standard);

        // rich
        Map<String, Object> rich = new LinkedHashMap<>();
        rich.put("sub", "usr_948271029");
        rich.put("name", "Alice W. Johnson");
        rich.put("email", "alice.johnson@example.com");
        rich.put("role", "USER");
        rich.put("department", "Infrastructure & SecOps");
        rich.put("title", "Senior Security Architect");
        rich.put("organization_id", "org_sec_884102");
        rich.put("permissions", List.of(
                "read:profile", "write:notes", "access:vault",
                "audit:logs:export", "infra:cluster:monitor", "secrets:rotate"
        ));
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("id", "dev_macbook_m3_8912");
        device.put("os", "macOS Sonoma 14.5");
        device.put("ip", "192.168.10.45");
        device.put("trusted", true);
        device.put("last_mfa", "2026-09-01T07:00:00Z");
        rich.put("device", device);
        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("country", "ID");
        geo.put("city", "Jakarta");
        geo.put("tz", "Asia/Jakarta");
        rich.put("geo", geo);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("department_code", "DPT-ENG-SEC");
        attributes.put("clearance_level", 4);
        attributes.put("session_affinity", "primary_datacenter_ap_southeast_3");
        rich.put("attributes", attributes);
        rich.put("iss", "https://auth.company.local");
        rich.put("aud", "api.company.local");
        PAYLOAD_PRESETS.put("rich", rich);

        // large
        Map<String, Object> large = new LinkedHashMap<>();
        large.put("sub", "usr_948271029");
        large.put("name", "Alice W. Johnson");
        large.put("email", "alice.johnson@example.com");
        large.put("role", "USER");
        large.put("department", "Infrastructure & SecOps");
        large.put("title", "Senior Security Architect");
        Map<String, Object> org = new LinkedHashMap<>();
        org.put("id", "org_enterprise_9921");
        org.put("name", "Global Financial Solutions Inc");
        org.put("tier", "ENTERPRISE_PLUS");
        org.put("sub_entities", List.of("apac_branch", "emea_hub", "us_east_datacenter"));
        large.put("organization", org);
        large.put("roles", List.of("USER", "DEVELOPER", "SEC_AUDITOR", "INCIDENT_RESPONDER"));
        List<String> perms = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            perms.add("resource:module_" + i + ":action_" + (i % 5));
        }
        large.put("permissions", perms);
        Map<String, Object> secCtx = new LinkedHashMap<>();
        secCtx.put("mfa_methods", List.of("fido2_webauthn", "totp", "hardware_key"));
        secCtx.put("risk_score", 0.02);
        secCtx.put("compliance_tags", List.of("SOC2_TYPE_II", "ISO27001", "PCI_DSS_3_2_1", "GDPR_EU", "OJK_POJK"));
        Map<String, Object> sessionPolicies = new LinkedHashMap<>();
        sessionPolicies.put("max_duration_seconds", 28800);
        sessionPolicies.put("idle_timeout_seconds", 1800);
        sessionPolicies.put("require_step_up_for", List.of("vault:modify", "billing:change", "keys:export"));
        secCtx.put("session_policies", sessionPolicies);
        large.put("security_context", secCtx);
        List<Map<String, String>> audit = new ArrayList<>();
        Map<String, String> a1 = new LinkedHashMap<>();
        a1.put("action", "login_password"); a1.put("status", "OK"); a1.put("at", "2026-09-01T06:45:10Z");
        Map<String, String> a2 = new LinkedHashMap<>();
        a2.put("action", "mfa_fido2_verify"); a2.put("status", "OK"); a2.put("at", "2026-09-01T06:45:22Z");
        Map<String, String> a3 = new LinkedHashMap<>();
        a3.put("action", "access_vault_token"); a3.put("status", "PENDING"); a3.put("at", "2026-09-01T07:12:00Z");
        audit.add(a1); audit.add(a2); audit.add(a3);
        large.put("audit_trail_preview", audit);
        large.put("iss", "https://auth.company.local");
        large.put("aud", "api.company.local");
        PAYLOAD_PRESETS.put("large", large);
    }

    public BenchmarkService(@Qualifier("jwtSecretKey") javax.crypto.SecretKey jwtSecretKey,
                            @Qualifier("pasetoV4SecretKey") SecretKey pasetoV4SecretKey,
                            @Qualifier("pasetoV4PrivateKey") PrivateKey pasetoV4PrivateKey,
                            @Qualifier("pasetoV4PublicKey") PublicKey pasetoV4PublicKey,
                            @Qualifier("pasetoV4KeyPair") java.security.KeyPair pasetoV4KeyPair,
                            ObjectMapper objectMapper) {
        this.jwtSecretKey = jwtSecretKey;
        this.pasetoV4SecretKey = pasetoV4SecretKey;
        this.pasetoV4PrivateKey = pasetoV4PrivateKey;
        this.pasetoV4PublicKey = pasetoV4PublicKey;
        this.pasetoV4KeyPair = pasetoV4KeyPair;
        this.objectMapper = objectMapper;
    }

    /**
     * Calculate latency statistics from nanosecond durations.
     * Returns values in milliseconds (ms).
     */
    public Map<String, Double> calculateStats(List<Long> latenciesNanos) {
        if (latenciesNanos.isEmpty()) {
            Map<String, Double> zero = new LinkedHashMap<>();
            zero.put("min", 0.0); zero.put("max", 0.0); zero.put("mean", 0.0);
            zero.put("p50", 0.0); zero.put("p95", 0.0); zero.put("p99", 0.0);
            return zero;
        }

        List<Double> ms = latenciesNanos.stream()
                .map(n -> n / 1_000_000.0)
                .sorted()
                .toList();

        double min = round3(ms.get(0));
        double max = round3(ms.get(ms.size() - 1));
        double mean = round3(ms.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        double p50 = round3(ms.get((int) Math.floor(ms.size() * 0.5)));
        double p95 = round3(ms.get((int) Math.floor(ms.size() * 0.95)));
        double p99 = round3(ms.get((int) Math.floor(ms.size() * 0.99)));

        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("min", min);
        stats.put("max", max);
        stats.put("mean", mean);
        stats.put("p50", p50);
        stats.put("p95", p95);
        stats.put("p99", p99);
        return stats;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public Map<String, Object> runBenchmark(Map<String, Object> options) throws Exception {
        int rounds = Math.max(10, Math.min(getInt(options, "rounds", 10), 50));
        String presetKey = getString(options, "preset", "standard");
        if (!PAYLOAD_PRESETS.containsKey(presetKey)) presetKey = "standard";

        Map<String, Object> basePayload = new LinkedHashMap<>(PAYLOAD_PRESETS.get(presetKey));
        basePayload.put("iat", System.currentTimeMillis() / 1000);

        String rawJson = objectMapper.writeValueAsString(basePayload);
        int rawPayloadBytes = rawJson.getBytes("UTF-8").length;



        // Warmup (300x each operation to trigger HotSpot C2 tier-4 JIT compilation)
        for (int i = 0; i < 300; i++) {
            String tw = buildJwt(basePayload);
            verifyJwt(tw);
            String twEd = buildJwtEddsa(basePayload);
            verifyJwtEddsa(twEd);
            String tp = buildPasetoLocal(rawJson);
            verifyPasetoLocal(tp);
            String tpub = buildPasetoPublic(rawJson);
            verifyPasetoPublic(tpub);
        }
        int requestedIter = getInt(options, "iterations", 1000);
        int opsPerRound = Math.max(10, Math.min((int) Math.round((double) requestedIter / rounds), 200));
        int totalOps = rounds * opsPerRound;

        List<Map<String, Object>> roundsHistory = new ArrayList<>();
        List<Long> allJwtSignLat = new ArrayList<>();
        List<Long> allJwtVerifyLat = new ArrayList<>();
        List<Long> allJwtEddsaSignLat = new ArrayList<>();
        List<Long> allJwtEddsaVerifyLat = new ArrayList<>();
        List<Long> allPasLocEncLat = new ArrayList<>();
        List<Long> allPasLocDecLat = new ArrayList<>();
        List<Long> allPasPubSignLat = new ArrayList<>();
        List<Long> allPasPubVerifyLat = new ArrayList<>();

        String sampleJwt = "";
        String sampleJwtEddsa = "";
        String samplePasLoc = "";
        String samplePasPub = "";
        for (int r = 0; r < rounds; r++) {
            // JWT HS256 sign
            long startJwtSign = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                sampleJwt = buildJwt(basePayload);
                allJwtSignLat.add(System.nanoTime() - t0);
            }
            long endJwtSign = System.nanoTime();
            double roundJwtSignMs = (endJwtSign - startJwtSign) / 1_000_000.0;

            // JWT HS256 verify
            long startJwtVerify = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                verifyJwt(sampleJwt);
                allJwtVerifyLat.add(System.nanoTime() - t0);
            }
            long endJwtVerify = System.nanoTime();
            double roundJwtVerifyMs = (endJwtVerify - startJwtVerify) / 1_000_000.0;

            // 2. JWT EdDSA (Ed25519 Asymmetric) sign & verify
            long startJwtEddsaSign = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                sampleJwtEddsa = buildJwtEddsa(basePayload);
                allJwtEddsaSignLat.add(System.nanoTime() - t0);
            }
            long endJwtEddsaSign = System.nanoTime();
            double roundJwtEddsaSignMs = (endJwtEddsaSign - startJwtEddsaSign) / 1_000_000.0;

            long startJwtEddsaVerify = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                verifyJwtEddsa(sampleJwtEddsa);
                allJwtEddsaVerifyLat.add(System.nanoTime() - t0);
            }
            long endJwtEddsaVerify = System.nanoTime();
            double roundJwtEddsaVerifyMs = (endJwtEddsaVerify - startJwtEddsaVerify) / 1_000_000.0;
            long startLocEnc = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                samplePasLoc = buildPasetoLocal(rawJson);
                allPasLocEncLat.add(System.nanoTime() - t0);
            }
            long endLocEnc = System.nanoTime();
            double roundLocEncMs = (endLocEnc - startLocEnc) / 1_000_000.0;

            // PASETO local decrypt
            long startLocDec = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                verifyPasetoLocal(samplePasLoc);
                allPasLocDecLat.add(System.nanoTime() - t0);
            }
            long endLocDec = System.nanoTime();
            double roundLocDecMs = (endLocDec - startLocDec) / 1_000_000.0;

            // PASETO public sign
            long startPubSign = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                samplePasPub = buildPasetoPublic(rawJson);
                allPasPubSignLat.add(System.nanoTime() - t0);
            }
            long endPubSign = System.nanoTime();
            double roundPubSignMs = (endPubSign - startPubSign) / 1_000_000.0;

            // PASETO public verify
            long startPubVerify = System.nanoTime();
            for (int i = 0; i < opsPerRound; i++) {
                long t0 = System.nanoTime();
                verifyPasetoPublic(samplePasPub);
                allPasPubVerifyLat.add(System.nanoTime() - t0);
            }
            long endPubVerify = System.nanoTime();
            double roundPubVerifyMs = (endPubVerify - startPubVerify) / 1_000_000.0;

            Map<String, Object> roundData = new LinkedHashMap<>();
            roundData.put("round", r + 1);

            Map<String, Object> jwtHsRound = new LinkedHashMap<>();
            jwtHsRound.put("signOpsSec", safeOps(opsPerRound, roundJwtSignMs));
            jwtHsRound.put("verifyOpsSec", safeOps(opsPerRound, roundJwtVerifyMs));
            jwtHsRound.put("roundtripOpsSec", safeOps(opsPerRound, roundJwtSignMs + roundJwtVerifyMs));
            double jwtLatMs = round3((roundJwtSignMs + roundJwtVerifyMs) / opsPerRound);
            jwtHsRound.put("avgLatencyMs", jwtLatMs);
            roundData.put("jwtHs", jwtHsRound);

            Map<String, Object> jwtEddsaRound = new LinkedHashMap<>();
            jwtEddsaRound.put("signOpsSec", safeOps(opsPerRound, roundJwtEddsaSignMs));
            jwtEddsaRound.put("verifyOpsSec", safeOps(opsPerRound, roundJwtEddsaVerifyMs));
            jwtEddsaRound.put("roundtripOpsSec", safeOps(opsPerRound, roundJwtEddsaSignMs + roundJwtEddsaVerifyMs));
            double jwtEddsaLatMs = round3((roundJwtEddsaSignMs + roundJwtEddsaVerifyMs) / opsPerRound);
            jwtEddsaRound.put("avgLatencyMs", jwtEddsaLatMs);
            roundData.put("jwtEddsa", jwtEddsaRound);
            Map<String, Object> pasetoLocRound = new LinkedHashMap<>();
            pasetoLocRound.put("encryptOpsSec", safeOps(opsPerRound, roundLocEncMs));
            pasetoLocRound.put("decryptOpsSec", safeOps(opsPerRound, roundLocDecMs));
            pasetoLocRound.put("roundtripOpsSec", safeOps(opsPerRound, roundLocEncMs + roundLocDecMs));
            double locLatMs = round3((roundLocEncMs + roundLocDecMs) / opsPerRound);
            pasetoLocRound.put("avgLatencyMs", locLatMs);
            roundData.put("pasetoLoc", pasetoLocRound);

            Map<String, Object> pasetoPubRound = new LinkedHashMap<>();
            pasetoPubRound.put("signOpsSec", safeOps(opsPerRound, roundPubSignMs));
            pasetoPubRound.put("verifyOpsSec", safeOps(opsPerRound, roundPubVerifyMs));
            pasetoPubRound.put("roundtripOpsSec", safeOps(opsPerRound, roundPubSignMs + roundPubVerifyMs));
            double pubLatMs = round3((roundPubSignMs + roundPubVerifyMs) / opsPerRound);
            pasetoPubRound.put("avgLatencyMs", pubLatMs);
            roundData.put("pasetoPub", pasetoPubRound);

            roundsHistory.add(roundData);
        }

        // Aggregated averages across rounds
        long avgJwtSignOps = avgLong(roundsHistory, "jwtHs", "signOpsSec");
        long avgJwtVerifyOps = avgLong(roundsHistory, "jwtHs", "verifyOpsSec");
        long avgJwtRoundtripOps = avgLong(roundsHistory, "jwtHs", "roundtripOpsSec");
        double avgJwtLatMs = avgDouble(roundsHistory, "jwtHs", "avgLatencyMs");

        long avgJwtEddsaSignOps = avgLong(roundsHistory, "jwtEddsa", "signOpsSec");
        long avgJwtEddsaVerifyOps = avgLong(roundsHistory, "jwtEddsa", "verifyOpsSec");
        long avgJwtEddsaRoundtripOps = avgLong(roundsHistory, "jwtEddsa", "roundtripOpsSec");
        double avgJwtEddsaLatMs = avgDouble(roundsHistory, "jwtEddsa", "avgLatencyMs");

        long avgLocEncOps = avgLong(roundsHistory, "pasetoLoc", "encryptOpsSec");
        long avgLocDecOps = avgLong(roundsHistory, "pasetoLoc", "decryptOpsSec");
        long avgLocRoundtripOps = avgLong(roundsHistory, "pasetoLoc", "roundtripOpsSec");
        double avgLocLatMs = avgDouble(roundsHistory, "pasetoLoc", "avgLatencyMs");

        long avgPubSignOps = avgLong(roundsHistory, "pasetoPub", "signOpsSec");
        long avgPubVerifyOps = avgLong(roundsHistory, "pasetoPub", "verifyOpsSec");
        long avgPubRoundtripOps = avgLong(roundsHistory, "pasetoPub", "roundtripOpsSec");
        double avgPubLatMs = avgDouble(roundsHistory, "pasetoPub", "avgLatencyMs");

        // JWT HS256 token stats
        int jwtByteSize = sampleJwt.getBytes("UTF-8").length;
        String[] jwtParts = sampleJwt.split("\\.");

        Map<String, Object> jwtHsStats = buildTokenStats(
                "JWT (HS256)", "Symmetric (HMAC-SHA256)", sampleJwt, jwtByteSize, rawPayloadBytes,
                new int[]{
                        jwtParts.length > 0 ? jwtParts[0].getBytes("UTF-8").length : 0,
                        jwtParts.length > 1 ? jwtParts[1].getBytes("UTF-8").length : 0,
                        jwtParts.length > 2 ? jwtParts[2].getBytes("UTF-8").length : 0
                },
                "sign", avgJwtSignOps, calculateStats(allJwtSignLat),
                "verify", avgJwtVerifyOps, calculateStats(allJwtVerifyLat),
                avgJwtRoundtripOps, avgJwtLatMs,
                true
        );

        // JWT EdDSA (Ed25519) token stats
        int jwtEddsaByteSize = sampleJwtEddsa.getBytes("UTF-8").length;
        String[] jwtEddsaParts = sampleJwtEddsa.split("\\.");
        Map<String, Object> jwtEddsaStats = buildTokenStats(
                "JWT (EdDSA / Ed25519)", "Asymmetric (Ed25519 / RFC 8037)",
                sampleJwtEddsa, jwtEddsaByteSize, rawPayloadBytes,
                new int[]{
                        jwtEddsaParts.length > 0 ? jwtEddsaParts[0].getBytes("UTF-8").length : 0,
                        jwtEddsaParts.length > 1 ? jwtEddsaParts[1].getBytes("UTF-8").length : 0,
                        jwtEddsaParts.length > 2 ? jwtEddsaParts[2].getBytes("UTF-8").length : 0
                },
                "sign", avgJwtEddsaSignOps, calculateStats(allJwtEddsaSignLat),
                "verify", avgJwtEddsaVerifyOps, calculateStats(allJwtEddsaVerifyLat),
                avgJwtEddsaRoundtripOps, avgJwtEddsaLatMs,
                true
        );
        int locByteSize = samplePasLoc.getBytes("UTF-8").length;
        Map<String, Object> pasetoLocStats = buildTokenStats(
                "PASETO (v4.local / XChaCha20 + BLAKE2b)", "Symmetric AEAD (XChaCha20 + BLAKE2b-MAC)",
                samplePasLoc, locByteSize, rawPayloadBytes,
                new int[]{9, locByteSize - 9 - 43, 43},
                "encrypt", avgLocEncOps, calculateStats(allPasLocEncLat),
                "decrypt", avgLocDecOps, calculateStats(allPasLocDecLat),
                avgLocRoundtripOps, avgLocLatMs,
                false
        );

        // PASETO public token stats
        int pubByteSize = samplePasPub.getBytes("UTF-8").length;
        Map<String, Object> pasetoPubStats = buildTokenStats(
                "PASETO (v4.public / Ed25519)", "Asymmetric (Ed25519 / EdDSA)",
                samplePasPub, pubByteSize, rawPayloadBytes,
                new int[]{10, pubByteSize - 10 - 86, 86},
                "sign", avgPubSignOps, calculateStats(allPasPubSignLat),
                "verify", avgPubVerifyOps, calculateStats(allPasPubVerifyLat),
                avgPubRoundtripOps, avgPubLatMs,
                true
        );

        // Environment info (read dynamically from running JVM & OS host)
        Map<String, Object> environment = getEnvironmentInfo();
        // Meta
        Map<String, Object> benchmarkMeta = new LinkedHashMap<>();
        benchmarkMeta.put("rounds", rounds);
        benchmarkMeta.put("iterationsPerRound", opsPerRound);
        benchmarkMeta.put("totalIterations", totalOps);
        benchmarkMeta.put("iterations", getInt(options, "iterations", totalOps));
        benchmarkMeta.put("preset", presetKey);
        benchmarkMeta.put("rawPayloadBytes", rawPayloadBytes);
        benchmarkMeta.put("timestamp", java.time.Instant.now().toString());
        benchmarkMeta.put("environment", environment);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("jwtHs", jwtHsStats);
        results.put("jwtEddsa", jwtEddsaStats);
        results.put("pasetoLoc", pasetoLocStats);
        results.put("pasetoPub", pasetoPubStats);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("benchmarkMeta", benchmarkMeta);
        response.put("roundsHistory", roundsHistory);
        response.put("payloadSample", basePayload);
        response.put("results", results);
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTokenStats(
            String name, String type, String token, int byteSize, int rawPayloadBytes,
            int[] structure,
            String op1Name, long op1Ops, Map<String, Double> op1Stats,
            String op2Name, long op2Ops, Map<String, Double> op2Stats,
            long roundtripOps, double avgLatUs,
            boolean isSignVerify) {

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("name", name);
        stats.put("type", type);
        stats.put("token", token);
        stats.put("charLength", token.length());
        stats.put("byteSize", byteSize);
        stats.put("rawPayloadBytes", rawPayloadBytes);
        stats.put("overheadBytes", byteSize - rawPayloadBytes);
        stats.put("overheadPercentage", round1(((double)(byteSize - rawPayloadBytes) / rawPayloadBytes) * 100));

        Map<String, Object> structBreakdown = new LinkedHashMap<>();
        structBreakdown.put("headerBytes", structure[0]);
        structBreakdown.put("payloadBytes", structure[1]);
        structBreakdown.put("signatureBytes", structure[2]);
        stats.put("structureBreakdown", structBreakdown);

        Map<String, Object> perf = new LinkedHashMap<>();
        Map<String, Object> op1 = new LinkedHashMap<>();
        op1.put("opsSec", op1Ops);
        op1.put("stats", op1Stats);
        perf.put(op1Name, op1);

        Map<String, Object> op2 = new LinkedHashMap<>();
        op2.put("opsSec", op2Ops);
        op2.put("stats", op2Stats);
        perf.put(op2Name, op2);

        Map<String, Object> roundtrip = new LinkedHashMap<>();
        roundtrip.put("opsSec", roundtripOps);
        roundtrip.put("avgLatencyMs", avgLatUs);
        perf.put("roundtrip", roundtrip);
        stats.put("performance", perf);

        return stats;
    }

    // --- internal crypto helpers ---

    private String buildJwt(Map<String, Object> payload) {
        var builder = Jwts.builder().signWith(jwtSecretKey, Jwts.SIG.HS256);
        for (var entry : payload.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        return builder.compact();
    }

    private void verifyJwt(String token) {
        Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(token);
    }

    private String buildJwtEddsa(Map<String, Object> payload) {
        var builder = Jwts.builder().signWith(pasetoV4KeyPair.getPrivate(), Jwts.SIG.EdDSA);
        for (var entry : payload.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        return builder.compact();
    }

    private void verifyJwtEddsa(String token) {
        Jwts.parser().verifyWith(pasetoV4KeyPair.getPublic()).build().parseSignedClaims(token);
    }

    private String buildPasetoLocal(String jsonPayload) throws Exception {
        return org.paseto4j.version4.Paseto.encrypt(pasetoV4SecretKey, jsonPayload, "");
    }

    private void verifyPasetoLocal(String token) throws Exception {
        org.paseto4j.version4.Paseto.decrypt(pasetoV4SecretKey, token, "");
    }

    private String buildPasetoPublic(String jsonPayload) throws Exception {
        return org.paseto4j.version4.Paseto.sign(pasetoV4PrivateKey, jsonPayload, "");
    }

    private void verifyPasetoPublic(String token) throws Exception {
        org.paseto4j.version4.Paseto.parse(pasetoV4PublicKey, token, "");
    }

    // --- utility helpers ---

    private long safeOps(int ops, double ms) {
        if (ms <= 0) return 0;
        return Math.round((ops / ms) * 1000);
    }

    @SuppressWarnings("unchecked")
    private long avgLong(List<Map<String, Object>> rounds, String group, String key) {
        return Math.round(rounds.stream()
                .mapToLong(r -> toLong(((Map<?, ?>) r.get(group)).get(key)))
                .average().orElse(0));
    }

    @SuppressWarnings("unchecked")
    private double avgDouble(List<Map<String, Object>> rounds, String group, String key) {
        return round3(rounds.stream()
                .mapToDouble(r -> toDouble(((Map<?, ?>) r.get(group)).get(key)))
                .average().orElse(0));
    }
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("javaVersion", System.getProperty("java.version"));
        environment.put("javaMajorVersion", System.getProperty("java.specification.version"));
        environment.put("javaVm", System.getProperty("java.vm.name"));
        environment.put("javaVendor", System.getProperty("java.vendor"));
        environment.put("springBootVersion", org.springframework.boot.SpringBootVersion.getVersion());
        environment.put("arch", System.getProperty("os.arch"));
        environment.put("platform", System.getProperty("os.name"));
        environment.put("osVersion", System.getProperty("os.version"));
        environment.put("cpus", Runtime.getRuntime().availableProcessors());
        environment.put("cpuModel", detectCpuModel());
        environment.put("maxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        return environment;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        if (map == null || !map.containsKey(key)) return defaultVal;
        try {
            return ((Number) map.get(key)).intValue();
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        if (map == null || !map.containsKey(key)) return defaultVal;
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }

    private double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String detectCpuModel() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            try {
                Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        return line.trim();
                    }
                }
            } catch (Exception ignored) {}
        } else if (os.contains("linux")) {
            try {
                java.nio.file.Path cpuInfo = java.nio.file.Path.of("/proc/cpuinfo");
                if (java.nio.file.Files.exists(cpuInfo)) {
                    for (String line : java.nio.file.Files.readAllLines(cpuInfo)) {
                        if (line.startsWith("model name")) {
                            return line.substring(line.indexOf(':') + 1).trim();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return System.getProperty("os.arch", "Unknown");
    }
}
