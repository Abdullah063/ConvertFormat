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
                null,
                null
        );

        assertEquals(ConversionType.DOCX_TO_PDF, upload.conversionType());
        assertEquals(".docx", upload.sourceExtension());
        assertNull(upload.quality());
    }

    @Test
    void acceptsPptxToPdf() {
        ValidatedUpload upload = validator.validate(
                file("presentation.pptx", bytes(0x50, 0x4b, 0x03, 0x04)),
                ConversionType.PPTX_TO_PDF,
                null
        );

        assertEquals(ConversionType.PPTX_TO_PDF, upload.conversionType());
        assertEquals(".pptx", upload.sourceExtension());
        assertNull(upload.quality());
    }

    @Test
    void acceptsXlsxToPdf() {
        ValidatedUpload upload = validator.validate(
                file("workbook.xlsx", bytes(0x50, 0x4b, 0x03, 0x04)),
                ConversionType.XLSX_TO_PDF,
                null
        );

        assertEquals(ConversionType.XLSX_TO_PDF, upload.conversionType());
        assertEquals(".xlsx", upload.sourceExtension());
        assertNull(upload.quality());
    }

    @Test
    void acceptsJpegWithRequestedQuality() {
        ValidatedUpload upload = validator.validate(
                file("photo.jpeg", bytes(0xff, 0xd8, 0xff, 0xe0)),
                ConversionType.IMAGE_TO_WEBP,
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
                ConversionType.IMAGE_TO_WEBP,
                null
        );

        assertEquals(ConversionType.IMAGE_TO_WEBP, upload.conversionType());
        assertEquals(".png", upload.sourceExtension());
        assertEquals(82, upload.quality());
    }

    @Test
    void acceptsWebpToJpegWithDefaultQuality() {
        ValidatedUpload upload = validator.validate(
                file("image.webp", webpSignature()),
                ConversionType.WEBP_TO_JPEG,
                null
        );

        assertEquals(ConversionType.WEBP_TO_JPEG, upload.conversionType());
        assertEquals(".webp", upload.sourceExtension());
        assertEquals(90, upload.quality());
    }

    @Test
    void acceptsWebpToPngWithoutQuality() {
        ValidatedUpload upload = validator.validate(
                file("image.webp", webpSignature()),
                ConversionType.WEBP_TO_PNG,
                40
        );

        assertEquals(ConversionType.WEBP_TO_PNG, upload.conversionType());
        assertNull(upload.quality());
    }

    @Test
    void acceptsJpegToPdfWithoutQuality() {
        ValidatedUpload upload = validator.validate(
                file("photo.jpg", bytes(0xff, 0xd8, 0xff)),
                ConversionType.IMAGE_TO_PDF,
                40
        );

        assertEquals(ConversionType.IMAGE_TO_PDF, upload.conversionType());
        assertNull(upload.quality());
    }

    @Test
    void acceptsPngToJpegWithDefaultQuality() {
        ValidatedUpload upload = validator.validate(
                file("image.png", bytes(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)),
                ConversionType.PNG_TO_JPEG,
                null
        );

        assertEquals(ConversionType.PNG_TO_JPEG, upload.conversionType());
        assertEquals(".png", upload.sourceExtension());
        assertEquals(90, upload.quality());
    }

    @Test
    void acceptsJpegToPngWithoutQuality() {
        ValidatedUpload upload = validator.validate(
                file("photo.jpeg", bytes(0xff, 0xd8, 0xff)),
                ConversionType.JPEG_TO_PNG,
                40
        );

        assertEquals(ConversionType.JPEG_TO_PNG, upload.conversionType());
        assertEquals(".jpeg", upload.sourceExtension());
        assertNull(upload.quality());
    }

    @Test
    void rejectsOfficeExtensionThatDoesNotMatchRequestedType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        file("presentation.pptx", bytes(0x50, 0x4b, 0x03, 0x04)),
                        ConversionType.XLSX_TO_PDF,
                        null
                )
        );
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        file("image.png", "not-a-png".getBytes()),
                        ConversionType.IMAGE_TO_WEBP,
                        82
                )
        );

        assertEquals("Seçilen dönüşüm türü bu dosya ile uyumlu değil", exception.getMessage());
    }

    @Test
    void rejectsQualityOutsideAcceptedRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        file("photo.jpg", bytes(0xff, 0xd8, 0xff)),
                        ConversionType.IMAGE_TO_WEBP,
                        101
                )
        );

        assertEquals("Görsel kalitesi 1 ile 100 arasında olmalıdır", exception.getMessage());
    }

    @Test
    void rejectsEmptyFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", new byte[0]), null, null)
        );

        assertEquals("Dosya boş olamaz", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.pdf", "content".getBytes()), null, null)
        );

        assertEquals(
                "Dönüşüm türü belirtilmeli ve desteklenen bir dosya yüklenmelidir",
                exception.getMessage()
        );
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] content = new byte[(int) ConversionFileValidator.MAX_FILE_SIZE + 1];

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", content), null, null)
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

    private byte[] webpSignature() {
        return bytes(
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        );
    }
}
