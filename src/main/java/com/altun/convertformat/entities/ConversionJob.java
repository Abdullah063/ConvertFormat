package com.altun.convertformat.entities;

import com.altun.convertformat.conversion.ConversionStatus;
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

    public ConversionJob( String originalFileName,String sourceStorageKey){
        this.originalFileName=originalFileName;
        this.sourceStorageKey=sourceStorageKey;
        this.status=ConversionStatus.PENDING;
        this.createdAt=LocalDateTime.now();

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
