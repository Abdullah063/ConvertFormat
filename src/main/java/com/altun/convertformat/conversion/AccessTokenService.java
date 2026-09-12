package com.altun.convertformat.conversion;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class AccessTokenService {

    private static final int TOKEN_SIZE_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_SIZE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        return HexFormat.of().formatHex(digest(token));
    }

    public boolean matches(String token, String expectedHash) {
        if (token == null || expectedHash == null) {
            return false;
        }

        try {
            return MessageDigest.isEqual(digest(token), HexFormat.of().parseHex(expectedHash));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algoritması kullanılamıyor", exception);
        }
    }
}
