package com.altun.convertformat.conversion.dto;

import com.altun.convertformat.conversion.ConversionJobCreation;
import com.altun.convertformat.conversion.ConversionStatus;
import com.altun.convertformat.conversion.ConversionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionJobCreatedResponseDto(
        UUID id,
        String originalFileName,
        ConversionType conversionType,
        Integer quality,
        ConversionStatus status,
        LocalDateTime createdAt,
        String downloadToken
) {

    public static ConversionJobCreatedResponseDto from(ConversionJobCreation creation) {
        return new ConversionJobCreatedResponseDto(
                creation.job().getId(),
                creation.job().getOriginalFileName(),
                creation.job().getConversionType(),
                creation.job().getQuality(),
                creation.job().getStatus(),
                creation.job().getCreatedAt(),
                creation.downloadToken()
        );
    }
}
