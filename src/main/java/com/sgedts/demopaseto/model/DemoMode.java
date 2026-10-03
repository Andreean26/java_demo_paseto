package com.sgedts.demopaseto.model;

public enum DemoMode {
    JWT, PASETO;

    public String toLowerString() {
        return name().toLowerCase();
    }

    public static DemoMode fromString(String s) {
        if (s == null) throw new IllegalArgumentException("Mode tidak boleh null");
        return switch (s.toLowerCase()) {
            case "jwt" -> JWT;
            case "paseto" -> PASETO;
            default -> throw new IllegalArgumentException("Mode harus jwt atau paseto, got: " + s);
        };
    }
}
