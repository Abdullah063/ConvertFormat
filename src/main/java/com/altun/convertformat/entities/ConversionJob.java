package com.altun.convertformat.entities;

import com.altun.convertformat.conversion.ConversionStatus;
import com.altun.convertformat.conversion.ConversionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversion_jobs")
public class ConversionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, unique = true, length = 255)
    private String sourceStorageKey;

    @Column(length = 255)
    private String resultStorageKey;

    @Column(nullable = false, unique = true, length = 64)
    private String accessTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversionType conversionType;

    private Integer quality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversionStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;


    protected ConversionJob(){

    }

    public ConversionJob(
            String originalFileName,
            String sourceStorageKey,
            String accessTokenHash,
            ConversionType conversionType,
            Integer quality
    ) {
        this.originalFileName = originalFileName;
        this.sourceStorageKey = sourceStorageKey;
        this.accessTokenHash = accessTokenHash;
        this.conversionType = conversionType;
        this.quality = quality;
        this.status = ConversionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public ConversionJob(
            String originalFileName,
            String sourceStorageKey,
            String accessTokenHash
    ) {
        this(
                originalFileName,
                sourceStorageKey,
                accessTokenHash,
                ConversionType.DOCX_TO_PDF,
                null
        );
    }



    public void markProcessing() {
        this.status = ConversionStatus.PROCESSING;
    }

    public void complete(String resultStorageKey) {
        this.resultStorageKey = resultStorageKey;
        this.status = ConversionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = ConversionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }



    //getter methods

    public UUID getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getSourceStorageKey() {
        return sourceStorageKey;
    }

    public String getResultStorageKey() {
        return resultStorageKey;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public ConversionType getConversionType() {
        return conversionType;
    }

    public Integer getQuality() {
        return quality;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ConversionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }


}
