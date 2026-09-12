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
import java.util.UUID;

@Service
public class ConversionJobService {

    private final ConversionJobRepository conversionJobRepository;
    private final FileStorage fileStorage;
    private final DocxFileValidator docxFileValidator;
    private final ApplicationEventPublisher eventPublisher;

    public ConversionJobService(
            ConversionJobRepository conversionJobRepository,
            FileStorage fileStorage,
            DocxFileValidator docxFileValidator,
            ApplicationEventPublisher eventPublisher
    ) {
        this.conversionJobRepository = conversionJobRepository;
        this.fileStorage = fileStorage;
        this.docxFileValidator = docxFileValidator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ConversionJob create(MultipartFile file) {
        docxFileValidator.validate(file);

        String storageKey = store(file);
        String originalFileName = extractFileName(file.getOriginalFilename());
        ConversionJob conversionJob = new ConversionJob(originalFileName, storageKey);

        try {
            ConversionJob savedJob = conversionJobRepository.saveAndFlush(conversionJob);
            eventPublisher.publishEvent(new ConversionJobCreatedEvent(savedJob.getId()));
            return savedJob;
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

    private String store(MultipartFile file) {
        try {
            return fileStorage.store(file);
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
}
