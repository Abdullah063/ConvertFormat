package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
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
    private DocumentConverter documentConverter;

    private ConversionJobProcessor conversionJobProcessor;

    @BeforeEach
    void setUp() {
        conversionJobProcessor = new ConversionJobProcessor(
                conversionJobRepository,
                documentConverter
        );
    }

    @Test
    void marksJobCompletedAfterSuccessfulConversion() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(conversionJobRepository.saveAndFlush(job)).thenReturn(job);
        when(documentConverter.convertToPdf("source.docx")).thenReturn("source.pdf");

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        assertEquals(ConversionStatus.COMPLETED, job.getStatus());
        assertEquals("source.pdf", job.getResultStorageKey());
        verify(conversionJobRepository).save(job);
    }

    @Test
    void marksJobFailedWhenConversionFails() {
        UUID id = UUID.randomUUID();
        ConversionJob job = new ConversionJob("example.docx", "source.docx");
        when(conversionJobRepository.findById(id)).thenReturn(Optional.of(job));
        when(conversionJobRepository.saveAndFlush(job)).thenReturn(job);
        when(documentConverter.convertToPdf("source.docx"))
                .thenThrow(new DocumentConversionException("LibreOffice çalıştırılamadı"));

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        assertEquals(ConversionStatus.FAILED, job.getStatus());
        assertEquals("Dosya PDF formatına dönüştürülemedi", job.getErrorMessage());
        verify(conversionJobRepository).save(job);
    }

    @Test
    void ignoresMissingJob() {
        UUID id = UUID.randomUUID();
        when(conversionJobRepository.findById(id)).thenReturn(Optional.empty());

        conversionJobProcessor.process(new ConversionJobCreatedEvent(id));

        verify(documentConverter, never()).convertToPdf("source.docx");
    }
}
