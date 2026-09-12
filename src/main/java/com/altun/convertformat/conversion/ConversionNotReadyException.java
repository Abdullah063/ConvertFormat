package com.altun.convertformat.conversion;

import java.util.UUID;

public class ConversionNotReadyException extends RuntimeException {

    public ConversionNotReadyException(UUID id) {
        super("Dönüşüm işlemi henüz tamamlanmadı: " + id);
    }
}
