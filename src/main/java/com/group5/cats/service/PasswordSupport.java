package com.group5.cats.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** New credentials use BCrypt; existing accounts keep working without a data rewrite. */
public final class PasswordSupport {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String PREFIX = "{bcrypt}";
    private PasswordSupport() {}

    public static String encode(String password) {
        return PREFIX + ENCODER.encode(password);
    }

    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        if (stored.startsWith(PREFIX)) return password.getBytes(StandardCharsets.UTF_8).length <= 72
                && ENCODER.matches(password, stored.substring(PREFIX.length()));
        return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }
}
