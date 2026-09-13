package com.altun.convertformat.conversion;

public enum ConversionType {
    DOCX_TO_PDF(".pdf", "application/pdf"),
    IMAGE_TO_WEBP(".webp", "image/webp");

    private final String outputExtension;
    private final String outputMediaType;

    ConversionType(String outputExtension, String outputMediaType) {
        this.outputExtension = outputExtension;
        this.outputMediaType = outputMediaType;
    }

    public String getOutputExtension() {
        return outputExtension;
    }

    public String getOutputMediaType() {
        return outputMediaType;
    }
}
