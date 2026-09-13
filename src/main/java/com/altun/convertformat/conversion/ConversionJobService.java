package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import com.altun.convertformat.storage.FileStorage;
import com.altun.convertformat.storage.FileStorageException;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ConversionJobService {

    private final ConversionJobRepository conversionJobRepository;
    private final FileStorage fileStorage;
    private final ConversionFileValidator conversionFileValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessTokenService accessTokenService;

    public ConversionJobService(
            ConversionJobRepository conversionJobRepository,
            FileStorage fileStorage,
            ConversionFileValidator conversionFileValidator,
            ApplicationEventPublisher eventPublisher,
            AccessTokenService accessTokenService
    ) {
        this.conversionJobRepository = conversionJobRepository;
        this.fileStorage = fileStorage;
        this.conversionFileValidator = conversionFileValidator;
        this.eventPublisher = eventPublisher;
        this.accessTokenService = accessTokenService;
    }

    @Transactional
    public ConversionJobCreation create(MultipartFile file, Integer quality) {
        ValidatedUpload upload = conversionFileValidator.validate(file, quality);

        String storageKey = store(file, upload.sourceExtension());
        String originalFileName = extractFileName(file.getOriginalFilename());
        String downloadToken = accessTokenService.generate();
        String accessTokenHash = accessTokenService.hash(downloadToken);
        ConversionJob conversionJob = new ConversionJob(
                originalFileName,
                storageKey,
                accessTokenHash,
                upload.conversionType(),
                upload.quality()
        );

        try {
            ConversionJob savedJob = conversionJobRepository.saveAndFlush(conversionJob);
            eventPublisher.publishEvent(new ConversionJobCreatedEvent(savedJob.getId()));
            return new ConversionJobCreation(savedJob, downloadToken);
        } catch (RuntimeException exception) {
            deleteAfterFailedSave(storageKey, exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ConversionJob findById(UUID id) {
        return conversionJobRepository.findById(id)
                .orElseThrow(() -> new ConversionJobNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public ConversionFile loadResult(UUID id, String downloadToken) {
        ConversionJob conversionJob = findById(id);

        if (!accessTokenService.matches(downloadToken, conversionJob.getAccessTokenHash())) {
            throw new InvalidAccessTokenException();
        }

        if (conversionJob.getStatus() != ConversionStatus.COMPLETED
                || conversionJob.getResultStorageKey() == null) {
            throw new ConversionNotReadyException(id);
        }

        Path result = fileStorage.load(conversionJob.getResultStorageKey());
        if (!Files.isRegularFile(result)) {
            throw new FileStorageException("Dönüştürülen dosya bulunamadı");
        }

        ConversionType conversionType = conversionJob.getConversionType();
        return new ConversionFile(
                result,
                replaceExtension(
                        conversionJob.getOriginalFileName(),
                        conversionType.getOutputExtension()
                ),
                conversionType.getOutputMediaType()
        );
    }

    private String store(MultipartFile file, String extension) {
        try {
            return fileStorage.store(file, extension);
        } catch (IOException exception) {
            throw new FileStorageException("Dosya kaydedilemedi", exception);
        }
    }

    private void deleteAfterFailedSave(String storageKey, RuntimeException originalException) {
        try {
            fileStorage.delete(storageKey);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private String extractFileName(String originalFileName) {
        String cleanedPath = StringUtils.cleanPath(originalFileName);
        int lastSeparator = cleanedPath.lastIndexOf('/');
        return cleanedPath.substring(lastSeparator + 1);
    }

    private String replaceExtension(String fileName, String extension) {
        int lastDot = fileName.lastIndexOf('.');
        String baseName = lastDot < 0 ? fileName : fileName.substring(0, lastDot);
        return baseName + extension;
    }
}
