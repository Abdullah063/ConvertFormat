package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConversionJobProcessor {

    private static final String FAILURE_MESSAGE = "Dosya PDF formatına dönüştürülemedi";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionJobProcessor.class);

    private final ConversionJobRepository conversionJobRepository;
    private final DocumentConverter documentConverter;

    public ConversionJobProcessor(
            ConversionJobRepository conversionJobRepository,
            DocumentConverter documentConverter
    ) {
        this.conversionJobRepository = conversionJobRepository;
        this.documentConverter = documentConverter;
    }

    @Async("conversionTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(ConversionJobCreatedEvent event) {
        conversionJobRepository.findById(event.jobId()).ifPresent(this::convert);
    }

    private void convert(ConversionJob conversionJob) {
        conversionJob.markProcessing();
        conversionJob = conversionJobRepository.saveAndFlush(conversionJob);

        try {
            String resultStorageKey = documentConverter.convertToPdf(
                    conversionJob.getSourceStorageKey()
            );
            conversionJob.complete(resultStorageKey);
        } catch (RuntimeException exception) {
            LOGGER.error("Conversion failed for job {}", conversionJob.getId(), exception);
            conversionJob.fail(FAILURE_MESSAGE);
        }

        conversionJobRepository.save(conversionJob);
    }
}
