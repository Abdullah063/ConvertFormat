package com.altun.convertformat.conversion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocxFileValidatorTest {

    private final DocxFileValidator validator = new DocxFileValidator();

    @Test
    void acceptsValidDocxFile() {
        MockMultipartFile file = file("example.docx", "content".getBytes());

        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void rejectsEmptyFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", new byte[0]))
        );

        assertEquals("Dosya boş olamaz", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.pdf", "content".getBytes()))
        );

        assertEquals("Yalnızca DOCX dosyaları kabul edilir", exception.getMessage());
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] content = new byte[(int) DocxFileValidator.MAX_FILE_SIZE + 1];

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(file("example.docx", content))
        );

        assertEquals("Dosya en fazla 10 MB olabilir", exception.getMessage());
    }

    private MockMultipartFile file(String fileName, byte[] content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                content
        );
    }
}
