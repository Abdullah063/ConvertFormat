package com.altun.convertformat.conversion;

import java.util.UUID;

public class ConversionJobNotFoundException extends RuntimeException {

    public ConversionJobNotFoundException(UUID id) {
        super("Dönüşüm işlemi bulunamadı: " + id);
    }
}
