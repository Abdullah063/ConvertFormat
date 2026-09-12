package com.altun.convertformat.conversion;

import com.altun.convertformat.conversion.dto.ConversionJobCreatedResponseDto;
import com.altun.convertformat.conversion.dto.ConversionJobResponseDto;
import com.altun.convertformat.entities.ConversionJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversions")
@Tag(name = "Conversions", description = "DOCX → PDF dönüşüm işlemleri")
public class ConversionJobController {

    private final ConversionJobService conversionJobService;

    public ConversionJobController(ConversionJobService conversionJobService) {
        this.conversionJobService = conversionJobService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Yeni bir DOCX → PDF dönüşümü başlatır")
    public ResponseEntity<ConversionJobCreatedResponseDto> create(
            @RequestPart("file") MultipartFile file
    ) {
        ConversionJobCreation creation = conversionJobService.create(file);
        ConversionJobCreatedResponseDto response = ConversionJobCreatedResponseDto.from(creation);

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/conversions/" + creation.job().getId()))
                .body(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Dönüşüm durumunu sorgular")
    public ConversionJobResponseDto findById(@PathVariable UUID id) {
        return ConversionJobResponseDto.from(conversionJobService.findById(id));
    }

    @GetMapping(value = "/{id}/file", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Tamamlanan PDF dosyasını gizli anahtarla indirir")
    public ResponseEntity<Resource> download(
            @PathVariable UUID id,
            @RequestHeader("X-Download-Token") String downloadToken
    ) {
        ConversionFile conversionFile = conversionJobService.loadResult(id, downloadToken);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(conversionFile.downloadFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", contentDisposition.toString())
                .body(new FileSystemResource(conversionFile.path()));
    }
}
