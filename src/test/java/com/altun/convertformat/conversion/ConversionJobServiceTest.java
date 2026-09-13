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
    private ConversionFileValidator conversionFileValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccessTokenService accessTokenService;

    private ConversionJobService conversionJobService;

    @BeforeEach
    void setUp() {
        conversionJobService = new ConversionJobService(
                conversionJobRepository,
                fileStorage,
                conversionFileValidator,
                eventPublisher,
                accessTokenService
        );
    }

    @Test
    void validatesStoresAndCreatesPendingJob() throws IOException {
        MockMultipartFile file = docx("folder/example.docx");
        when(conversionFileValidator.validate(file, null)).thenReturn(
                new ValidatedUpload(ConversionType.DOCX_TO_PDF, ".docx", null)
        );
        when(fileStorage.store(file, ".docx")).thenReturn("generated.docx");
        when(accessTokenService.generate()).thenReturn("download-token");
        when(accessTokenService.hash("download-token")).thenReturn("token-hash");
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenAnswer(invocation -> {
                    ConversionJob job = invocation.getArgument(0);
                    ReflectionTestUtils.setField(job, "id", id);
                    return job;
                });

        ConversionJobCreation creation = conversionJobService.create(file, null);
        ConversionJob result = creation.job();

        assertEquals("example.docx", result.getOriginalFileName());
        assertEquals("generated.docx", result.getSourceStorageKey());
        assertEquals(ConversionStatus.PENDING, result.getStatus());
        assertEquals(ConversionType.DOCX_TO_PDF, result.getConversionType());
        assertEquals("download-token", creation.downloadToken());
        assertEquals("token-hash", result.getAccessTokenHash());

        InOrder order = inOrder(conversionFileValidator, fileStorage, conversionJobRepository);
        order.verify(conversionFileValidator).validate(file, null);
        order.verify(fileStorage).store(file, ".docx");
        order.verify(conversionJobRepository).saveAndFlush(any(ConversionJob.class));
        verify(eventPublisher).publishEvent(new ConversionJobCreatedEvent(id));
    }

    @Test
    void createsWebpJobWithRequestedQuality() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47}
        );
        when(conversionFileValidator.validate(file, 76)).thenReturn(
                new ValidatedUpload(ConversionType.IMAGE_TO_WEBP, ".png", 76)
        );
        when(fileStorage.store(file, ".png")).thenReturn("generated.png");
        when(accessTokenService.generate()).thenReturn("download-token");
        when(accessTokenService.hash("download-token")).thenReturn("token-hash");
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenAnswer(invocation -> {
                    ConversionJob job = invocation.getArgument(0);
                    ReflectionTestUtils.setField(job, "id", id);
                    return job;
                });

        ConversionJob result = conversionJobService.create(file, 76).job();

        assertEquals(ConversionType.IMAGE_TO_WEBP, result.getConversionType());
        assertEquals(76, result.getQuality());
        assertEquals("generated.png", result.getSourceStorageKey());
        verify(eventPublisher).publishEvent(new ConversionJobCreatedEvent(id));
    }

    @Test
    void wrapsStorageFailureAndDoesNotSaveJob() throws IOException {
        MockMultipartFile file = docx("example.docx");
        when(conversionFileValidator.validate(file, null)).thenReturn(
                new ValidatedUpload(ConversionType.DOCX_TO_PDF, ".docx", null)
        );
        when(fileStorage.store(file, ".docx")).thenThrow(new IOException("disk full"));

        FileStorageException exception = assertThrows(
                FileStorageException.class,
                () -> conversionJobService.create(file, null)
        );

        assertEquals("Dosya kaydedilemedi", exception.getMessage());
        verify(conversionJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void deletesStoredFileWhenDatabaseSaveFails() throws IOException {
        MockMultipartFile file = docx("example.docx");
        when(conversionFileValidator.validate(file, null)).thenReturn(
                new ValidatedUpload(ConversionType.DOCX_TO_PDF, ".docx", null)
        );
        when(fileStorage.store(file, ".docx")).thenReturn("generated.docx");
        when(conversionJobRepository.saveAndFlush(any(ConversionJob.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(
                IllegalStateException.class,
                () -> conversionJobService.create(file, null)
        );

        verify(fileStorage).delete("generated.docx");
    }

    @Test
    void returnsJobById() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "generated.docx", "token-hash");
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
        ConversionJob job = new ConversionJob("my-document.docx", "source.docx", "token-hash");
        job.complete("source.pdf");
        Path pdf = Files.writeString(tempDirectory.resolve("source.pdf"), "pdf");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(accessTokenService.matches("download-token", "token-hash")).thenReturn(true);
        when(fileStorage.load("source.pdf")).thenReturn(pdf);

        ConversionFile result = conversionJobService.loadResult(id, "download-token");

        assertEquals(pdf, result.path());
        assertEquals("my-document.pdf", result.downloadFileName());
        assertEquals("application/pdf", result.mediaType());
    }

    @Test
    void loadsCompletedWebpImage() throws IOException {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob(
                "my-photo.jpeg",
                "source.jpeg",
                "token-hash",
                ConversionType.IMAGE_TO_WEBP,
                82
        );
        job.complete("source.webp");
        Path webp = Files.writeString(tempDirectory.resolve("source.webp"), "webp");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(accessTokenService.matches("download-token", "token-hash")).thenReturn(true);
        when(fileStorage.load("source.webp")).thenReturn(webp);

        ConversionFile result = conversionJobService.loadResult(id, "download-token");

        assertEquals(webp, result.path());
        assertEquals("my-photo.webp", result.downloadFileName());
        assertEquals("image/webp", result.mediaType());
    }

    @Test
    void rejectsDownloadWhileConversionIsPending() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx", "token-hash");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(accessTokenService.matches("download-token", "token-hash")).thenReturn(true);

        assertThrows(
                ConversionNotReadyException.class,
                () -> conversionJobService.loadResult(id, "download-token")
        );
        verify(fileStorage, never()).load(any());
    }

    @Test
    void rejectsInvalidDownloadToken() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx", "token-hash");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(accessTokenService.matches("wrong-token", "token-hash")).thenReturn(false);

        assertThrows(
                InvalidAccessTokenException.class,
                () -> conversionJobService.loadResult(id, "wrong-token")
        );
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
