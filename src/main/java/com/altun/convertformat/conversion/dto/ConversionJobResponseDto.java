package com.altun.convertformat.conversion.dto;

import com.altun.convertformat.conversion.ConversionStatus;
import com.altun.convertformat.conversion.ConversionType;
import com.altun.convertformat.entities.ConversionJob;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionJobResponseDto(
        UUID id,
        String originalFileName,
        ConversionType conversionType,
        Integer quality,
        ConversionStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String errorMessage
) {

    public static ConversionJobResponseDto from(ConversionJob conversionJob) {
        return new ConversionJobResponseDto(
                conversionJob.getId(),
                conversionJob.getOriginalFileName(),
                conversionJob.getConversionType(),
                conversionJob.getQuality(),
                conversionJob.getStatus(),
                conversionJob.getCreatedAt(),
                conversionJob.getCompletedAt(),
                conversionJob.getErrorMessage()
        );
    }
}
