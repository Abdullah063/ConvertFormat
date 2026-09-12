package com.altun.convertformat.conversion;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Geçersiz veya eksik indirme anahtarı");
    }
}
