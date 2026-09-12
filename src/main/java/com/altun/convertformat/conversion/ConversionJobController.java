package com.altun.convertformat.conversion;

import com.altun.convertformat.conversion.dto.ConversionJobResponseDto;
import com.altun.convertformat.entities.ConversionJob;
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
}
