package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import com.altun.convertformat.storage.FileStorage;
import com.altun.convertformat.storage.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionJobServiceTest {

    @TempDir
    private Path tempDirectory;

    @Mock
    private ConversionJobRepository conversionJobRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DocxFileValidator docxFileValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConversionJobService conversionJobService;

    @BeforeEach
    void setUp() {
        conversionJobService = new ConversionJobService(
                conversionJobRepository,
                fileStorage,
                docxFileValidator,
                eventPublisher
        );
    }

    @Test
    void validatesStoresAndCreatesPendingJob() throws IOException {
        MockMultipartFile file = docx("folder/example.docx");
        when(fileStorage.store(file)).thenReturn("generated.docx");
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenAnswer(invocation -> {
                    ConversionJob job = invocation.getArgument(0);
                    ReflectionTestUtils.setField(job, "id", id);
                    return job;
                });

        ConversionJob result = conversionJobService.create(file);

        assertEquals("example.docx", result.getOriginalFileName());
        assertEquals("generated.docx", result.getSourceStorageKey());
        assertEquals(ConversionStatus.PENDING, result.getStatus());

        InOrder order = inOrder(docxFileValidator, fileStorage, conversionJobRepository);
        order.verify(docxFileValidator).validate(file);
        order.verify(fileStorage).store(file);
        order.verify(conversionJobRepository).saveAndFlush(any(ConversionJob.class));
        verify(eventPublisher).publishEvent(new ConversionJobCreatedEvent(id));
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

    @Test
    void returnsJobById() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "generated.docx");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));

        ConversionJob result = conversionJobService.findById(id);

        assertEquals(job, result);
    }

    @Test
    void throwsNotFoundWhenJobDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.findById(id)).thenReturn(Optional.empty());

        ConversionJobNotFoundException exception = assertThrows(
                ConversionJobNotFoundException.class,
                () -> conversionJobService.findById(id)
        );

        assertEquals("Dönüşüm işlemi bulunamadı: " + id, exception.getMessage());
    }

    @Test
    void loadsCompletedPdf() throws IOException {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("my-document.docx", "source.docx");
        job.complete("source.pdf");
        Path pdf = Files.writeString(tempDirectory.resolve("source.pdf"), "pdf");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(fileStorage.load("source.pdf")).thenReturn(pdf);

        ConversionFile result = conversionJobService.loadResult(id);

        assertEquals(pdf, result.path());
        assertEquals("my-document.pdf", result.downloadFileName());
    }

    @Test
    void rejectsDownloadWhileConversionIsPending() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThrows(ConversionNotReadyException.class, () -> conversionJobService.loadResult(id));
        verify(fileStorage, never()).load(any());
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
