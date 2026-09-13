package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionJobProcessorTest {

    @Mock
    private ConversionJobRepository conversionJobRepository;

    @Mock
    private ConversionEngine conversionEngine;

    private ConversionJobProcessor conversionJobProcessor;

    @BeforeEach
    void setUp() {
        when(conversionEngine.supportedType()).thenReturn(ConversionType.DOCX_TO_PDF);
        conversionJobProcessor = new ConversionJobProcessor(
                conversionJobRepository,
                List.of(conversionEngine)
        );
    }

    @Test
    void marksJobCompletedAfterSuccessfulConversion() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx", "token-hash");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(conversionJobRepository.saveAndFlush(job)).thenReturn(job);
        when(conversionEngine.convert(job)).thenReturn("source.pdf");

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        assertEquals(ConversionStatus.COMPLETED, job.getStatus());
        assertEquals("source.pdf", job.getResultStorageKey());
        verify(conversionJobRepository).save(job);
    }

    @Test
    void marksJobFailedWhenConversionFails() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx", "token-hash");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(conversionJobRepository.saveAndFlush(job)).thenReturn(job);
        when(conversionEngine.convert(job))
                .thenThrow(new DocumentConversionException("LibreOffice çalıştırılamadı"));

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        assertEquals(ConversionStatus.FAILED, job.getStatus());
        assertEquals("Dosya dönüştürülemedi", job.getErrorMessage());
        verify(conversionJobRepository).save(job);
    }

    @Test
    void ignoresMissingJob() {
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.findById(id)).thenReturn(Optional.empty());

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        verify(conversionEngine, never()).convert(org.mockito.ArgumentMatchers.any());
    }
}
