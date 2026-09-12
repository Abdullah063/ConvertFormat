package com.altun.convertformat.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreUploadedFile() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(
                tempDirectory.toString()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "test-content".getBytes()
        );

        String storageKey = storage.store(file);
        Path storedFile = storage.load(storageKey);

        assertTrue(Files.exists(storedFile));
        assertEquals("test-content", Files.readString(storedFile));
    }

    @Test
    void shouldRejectPathTraversal() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(tempDirectory.toString());

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.load("../secret.docx")
        );
    }
}
