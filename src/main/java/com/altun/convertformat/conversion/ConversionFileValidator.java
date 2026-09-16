package com.altun.convertformat.conversion;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class ConversionFileValidator {

    static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    static final int DEFAULT_WEBP_QUALITY = 82;
    static final int DEFAULT_JPEG_QUALITY = 90;

    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };
    private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50};

    public ValidatedUpload validate(
            MultipartFile file,
            ConversionType requestedType,
            Integer requestedQuality
    ) {
        validateSize(file);

        String extension = extensionOf(file.getOriginalFilename());
        byte[] signature = readSignature(file);
        ConversionType conversionType = requestedType == null
                ? inferConversionType(extension)
                : requestedType;

        return switch (conversionType) {
            case DOCX_TO_PDF -> validateDocx(extension, signature);
            case PPTX_TO_PDF -> validateOfficeDocument(
                    extension,
                    ".pptx",
                    signature,
                    ConversionType.PPTX_TO_PDF
            );
            case XLSX_TO_PDF -> validateOfficeDocument(
                    extension,
                    ".xlsx",
                    signature,
                    ConversionType.XLSX_TO_PDF
            );
            case IMAGE_TO_WEBP -> validateJpegOrPng(
                    extension,
                    signature,
                    requestedQuality,
                    DEFAULT_WEBP_QUALITY,
                    ConversionType.IMAGE_TO_WEBP
            );
            case IMAGE_TO_PDF -> validateJpegOrPng(
                    extension,
                    signature,
                    null,
                    0,
                    ConversionType.IMAGE_TO_PDF
            );
            case WEBP_TO_JPEG -> validateWebp(
                    extension,
                    signature,
                    requestedQuality,
                    DEFAULT_JPEG_QUALITY,
                    ConversionType.WEBP_TO_JPEG
            );
            case WEBP_TO_PNG -> validateWebp(
                    extension,
                    signature,
                    null,
                    0,
                    ConversionType.WEBP_TO_PNG
            );
            case PNG_TO_JPEG -> validatePng(
                    extension,
                    signature,
                    requestedQuality,
                    ConversionType.PNG_TO_JPEG
            );
            case JPEG_TO_PNG -> validateJpeg(
                    extension,
                    signature,
                    ConversionType.JPEG_TO_PNG
            );
        };
    }

    private void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya en fazla 10 MB olabilir");
        }
    }

    private ValidatedUpload validateDocx(String extension, byte[] signature) {
        return validateOfficeDocument(
                extension,
                ".docx",
                signature,
                ConversionType.DOCX_TO_PDF
        );
    }

    private ValidatedUpload validateOfficeDocument(
            String extension,
            String expectedExtension,
            byte[] signature,
            ConversionType conversionType
    ) {
        if (!extension.equals(expectedExtension) || !startsWith(signature, ZIP_SIGNATURE)) {
            throw incompatibleFile();
        }

        return new ValidatedUpload(conversionType, expectedExtension, null);
    }

    private ValidatedUpload validateJpegOrPng(
            String extension,
            byte[] signature,
            Integer requestedQuality,
            int defaultQuality,
            ConversionType conversionType
    ) {
        boolean validJpeg = (extension.equals(".jpg") || extension.equals(".jpeg"))
                && startsWith(signature, JPEG_SIGNATURE);
        boolean validPng = extension.equals(".png") && startsWith(signature, PNG_SIGNATURE);

        if (!validJpeg && !validPng) {
            throw incompatibleFile();
        }

        Integer quality = defaultQuality == 0
                ? null
                : validateQuality(requestedQuality, defaultQuality);
        return new ValidatedUpload(conversionType, extension, quality);
    }

    private ValidatedUpload validateWebp(
            String extension,
            byte[] signature,
            Integer requestedQuality,
            int defaultQuality,
            ConversionType conversionType
    ) {
        if (!extension.equals(".webp") || !isWebp(signature)) {
            throw incompatibleFile();
        }

        Integer quality = defaultQuality == 0
                ? null
                : validateQuality(requestedQuality, defaultQuality);
        return new ValidatedUpload(conversionType, ".webp", quality);
    }

    private ValidatedUpload validatePng(
            String extension,
            byte[] signature,
            Integer requestedQuality,
            ConversionType conversionType
    ) {
        if (!extension.equals(".png") || !startsWith(signature, PNG_SIGNATURE)) {
            throw incompatibleFile();
        }

        return new ValidatedUpload(
                conversionType,
                ".png",
                validateQuality(requestedQuality, DEFAULT_JPEG_QUALITY)
        );
    }

    private ValidatedUpload validateJpeg(
            String extension,
            byte[] signature,
            ConversionType conversionType
    ) {
        boolean validExtension = extension.equals(".jpg") || extension.equals(".jpeg");
        if (!validExtension || !startsWith(signature, JPEG_SIGNATURE)) {
            throw incompatibleFile();
        }

        return new ValidatedUpload(conversionType, extension, null);
    }

    private byte[] readSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(12);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Dosya okunamadı", exception);
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }

        String normalized = fileName.toLowerCase(Locale.ROOT);
        int lastDot = normalized.lastIndexOf('.');
        return lastDot < 0 ? "" : normalized.substring(lastDot);
    }

    private ConversionType inferConversionType(String extension) {
        return switch (extension) {
            case ".docx" -> ConversionType.DOCX_TO_PDF;
            case ".jpg", ".jpeg", ".png" -> ConversionType.IMAGE_TO_WEBP;
            default -> throw new IllegalArgumentException(
                    "Dönüşüm türü belirtilmeli ve desteklenen bir dosya yüklenmelidir"
            );
        };
    }

    private int validateQuality(Integer requestedQuality, int defaultQuality) {
        int quality = requestedQuality == null ? defaultQuality : requestedQuality;
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Görsel kalitesi 1 ile 100 arasında olmalıdır");
        }
        return quality;
    }

    private boolean isWebp(byte[] signature) {
        if (signature.length < 12 || !startsWith(signature, RIFF_SIGNATURE)) {
            return false;
        }

        for (int index = 0; index < WEBP_SIGNATURE.length; index++) {
            if (signature[index + 8] != WEBP_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private IllegalArgumentException incompatibleFile() {
        return new IllegalArgumentException("Seçilen dönüşüm türü bu dosya ile uyumlu değil");
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) {
            return false;
        }

        for (int index = 0; index < expected.length; index++) {
            if (actual[index] != expected[index]) {
                return false;
            }
        }

        return true;
    }
}
