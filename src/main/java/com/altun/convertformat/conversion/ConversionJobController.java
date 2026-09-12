package com.altun.convertformat.conversion;

import com.altun.convertformat.conversion.dto.ConversionJobResponseDto;
import com.altun.convertformat.entities.ConversionJob;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversions")
public class ConversionJobController {

    private final ConversionJobService conversionJobService;

    public ConversionJobController(ConversionJobService conversionJobService) {
        this.conversionJobService = conversionJobService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConversionJobResponseDto> create(
            @RequestPart("file") MultipartFile file
    ) {
        ConversionJob conversionJob = conversionJobService.create(file);
        ConversionJobResponseDto response = ConversionJobResponseDto.from(conversionJob);

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/conversions/" + conversionJob.getId()))
                .body(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ConversionJobResponseDto findById(@PathVariable UUID id) {
        return ConversionJobResponseDto.from(conversionJobService.findById(id));
    }

    @GetMapping(value = "/{id}/file", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        ConversionFile conversionFile = conversionJobService.loadResult(id);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(conversionFile.downloadFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", contentDisposition.toString())
                .body(new FileSystemResource(conversionFile.path()));
    }
}
