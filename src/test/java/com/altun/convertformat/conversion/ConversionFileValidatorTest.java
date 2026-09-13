package com.altun.convertformat.conversion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionFileValidatorTest {

    private final ConversionFileValidator validator = new ConversionFileValidator();

    @Test
    void acceptsDocxFile() {
        ValidatedUpload upload = validator.validate(
                file("example.docx", bytes(0x50, 0x4b, 0x03, 0x04)),
                null
        );

        assertEquals(ConversionType.DOCX_TO_PDF, upload.conversionType());
        assertEquals(".docx", upload.sourceExtension());
        assertNull(upload.quality());
    }

    @Test
    void acceptsJpegWithRequestedQuality() {
        ValidatedUpload upload = validator.validate(
                file("photo.jpeg", bytes(0xff, 0xd8, 0xff, 0xe0)),
                74
        );

        assertEquals(ConversionType.IMAGE_TO_WEBP, upload.conversionType());
        assertEquals(".jpeg", upload.sourceExtension());
        assertEquals(74, upload.quality());
    }

    @Test
    void acceptsPngWithDefaultQuality() {
        ValidatedUpload upload = validator.validate(
                file("image.png", bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)),
                null
        );

        assertEquals(ConversionType.IMAGE_TO_WEBP, upload.conversionType());
        assertEquals(".png", upload.sourceExtension());
        assertEquals(82, upload.quality());
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("image.png", "not-a-png".getBytes()), 82)
        );

        assertEquals("Geçersiz PNG dosyası", exception.getMessage());
    }

    @Test
    void rejectsQualityOutsideAcceptedRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("photo.jpg", bytes(0xff, 0xd8, 0xff)), 101)
        );

        assertEquals("WebP kalitesi 1 ile 100 arasında olmalıdır", exception.getMessage());
    }

    @Test
    void rejectsEmptyFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", new byte[0]), null)
        );

        assertEquals("Dosya boş olamaz", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.pdf", "content".getBytes()), null)
        );

        assertEquals("Yalnızca DOCX, JPEG ve PNG dosyaları kabul edilir", exception.getMessage());
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] content = new byte[(int) ConversionFileValidator.MAX_FILE_SIZE + 1];

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", content), null)
        );

        assertEquals("Dosya en fazla 10 MB olabilir", exception.getMessage());
    }

    private MockMultipartFile file(String fileName, byte[] content) {
        return new MockMultipartFile("file", fileName, "application/octet-stream", content);
    }

    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
