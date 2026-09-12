package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import com.altun.convertformat.storage.FileStorage;
import com.altun.convertformat.storage.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionJobServiceTest {

    @Mock
    private ConversionJobRepository conversionJobRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DocxFileValidator docxFileValidator;

    private ConversionJobService conversionJobService;

    @BeforeEach
    void setUp() {
        conversionJobService = new ConversionJobService(
                conversionJobRepository,
                fileStorage,
                docxFileValidator
        );
    }

    @Test
    void validatesStoresAndCreatesPendingJob() throws IOException {
        MockMultipartFile file = docx("folder/example.docx");
        when(fileStorage.store(file)).thenReturn("generated.docx");
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConversionJob result = conversionJobService.create(file);

        assertEquals("example.docx", result.getOriginalFileName());
        assertEquals("generated.docx", result.getSourceStorageKey());
        assertEquals(ConversionStatus.PENDING, result.getStatus());

        InOrder order = inOrder(docxFileValidator, fileStorage, conversionJobRepository);
        order.verify(docxFileValidator).validate(file);
        order.verify(fileStorage).store(file);
        order.verify(conversionJobRepository).saveAndFlush(any(ConversionJob.class));
    }

    @Test
    void wrapsStorageFailureAndDoesNotSaveJob() throws IOException {
        MockMultipartFile file = docx("example.docx");
        when(fileStorage.store(file)).thenThrow(new IOException("disk full"));

        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> conversionJobService.create(file)
        );

        assertEquals("Dosya kaydedilemedi", exception.getMessage());
        verify(conversionJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void deletesStoredFileWhenDatabaseSaveFails() throws IOException {
        MockMultipartFile file = docx("example.docx");
        when(fileStorage.store(file)).thenReturn("generated.docx");
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> conversionJobService.create(file)
        );

        verify(fileStorage).delete("generated.docx");
    }

    private MockMultipartFile docx(String fileName) {
        return new MockMultipartFile(
                "file",
                fileName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "content".getBytes()
        );
    }
}
