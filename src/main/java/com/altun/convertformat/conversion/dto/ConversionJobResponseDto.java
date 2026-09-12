package com.altun.convertformat.conversion.dto;

import com.altun.convertformat.conversion.ConversionStatus;
import com.altun.convertformat.entities.ConversionJob;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionJobResponseDto(
        UUID id,
        String originalFileName,
        ConversionStatus status,
        LocalDateTime createdAt
) {

    public static ConversionJobResponseDto from(ConversionJob conversionJob) {
        return new ConversionJobResponseDto(
                conversionJob.getId(),
                conversionJob.getOriginalFileName(),
                conversionJob.getStatus(),
                conversionJob.getCreatedAt()
        );
    }
}
