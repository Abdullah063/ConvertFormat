package com.altun.convertformat.conversion.dto;

import com.altun.convertformat.conversion.ConversionJobCreation;
import com.altun.convertformat.conversion.ConversionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionJobCreatedResponseDto(
        UUID id,
        String originalFileName,
        ConversionStatus status,
        LocalDateTime createdAt,
        String downloadToken
) {

    public static ConversionJobCreatedResponseDto from(ConversionJobCreation creation) {
        return new ConversionJobCreatedResponseDto(
                creation.job().getId(),
                creation.job().getOriginalFileName(),
                creation.job().getStatus(),
                creation.job().getCreatedAt(),
                creation.downloadToken()
        );
    }
}
