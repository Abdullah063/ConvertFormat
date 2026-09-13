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

    private static final byte[] DOCX_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    public ValidatedUpload validate(MultipartFile file, Integer requestedQuality) {
        validateSize(file);

        String extension = extensionOf(file.getOriginalFilename());
        byte[] signature = readSignature(file);

        return switch (extension) {
            case ".docx" -> validateDocx(signature);
            case ".jpg", ".jpeg" -> validateImage(
                    signature,
                    JPEG_SIGNATURE,
                    extension,
                    requestedQuality,
                    "Geçersiz JPEG dosyası"
            );
            case ".png" -> validateImage(
                    signature,
                    PNG_SIGNATURE,
                    extension,
                    requestedQuality,
                    "Geçersiz PNG dosyası"
            );
            default -> throw new IllegalArgumentException(
                    "Yalnızca DOCX, JPEG ve PNG dosyaları kabul edilir"
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

    private ValidatedUpload validateDocx(byte[] signature) {
        if (!startsWith(signature, DOCX_SIGNATURE)) {
            throw new IllegalArgumentException("Geçersiz DOCX dosyası");
        }

        return new ValidatedUpload(ConversionType.DOCX_TO_PDF, ".docx", null);
    }

    private ValidatedUpload validateImage(
            byte[] signature,
            byte[] expectedSignature,
            String extension,
            Integer requestedQuality,
            String errorMessage
    ) {
        if (!startsWith(signature, expectedSignature)) {
            throw new IllegalArgumentException(errorMessage);
        }

        int quality = requestedQuality == null ? DEFAULT_WEBP_QUALITY : requestedQuality;
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("WebP kalitesi 1 ile 100 arasında olmalıdır");
        }

        return new ValidatedUpload(ConversionType.IMAGE_TO_WEBP, extension, quality);
    }

    private byte[] readSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(PNG_SIGNATURE.length);
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
