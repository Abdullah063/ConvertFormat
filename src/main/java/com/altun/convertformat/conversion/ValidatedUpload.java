package com.altun.convertformat.conversion;

public record ValidatedUpload(
        ConversionType conversionType,
        String sourceExtension,
        Integer quality
) {
}
