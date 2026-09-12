package com.altun.convertformat.conversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenServiceTest {

    private final AccessTokenService accessTokenService = new AccessTokenService();

    @Test
    void generatesUniqueUrlSafeTokens() {
        String first = accessTokenService.generate();
        String second = accessTokenService.generate();

        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void verifiesTokenAgainstHash() {
        String hash = accessTokenService.hash("secret-token");

        assertTrue(accessTokenService.matches("secret-token", hash));
        assertFalse(accessTokenService.matches("wrong-token", hash));
        assertFalse(accessTokenService.matches(null, hash));
    }
}
