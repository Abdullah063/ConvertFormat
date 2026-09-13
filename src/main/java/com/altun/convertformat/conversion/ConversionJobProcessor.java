package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.repositories.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ConversionJobProcessor {

    private static final String FAILURE_MESSAGE = "Dosya dönüştürülemedi";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionJobProcessor.class);

    private final ConversionJobRepository conversionJobRepository;
    private final Map<ConversionType, ConversionEngine> conversionEngines;

    public ConversionJobProcessor(
            ConversionJobRepository conversionJobRepository,
            List<ConversionEngine> conversionEngines
    ) {
        this.conversionJobRepository = conversionJobRepository;
        this.conversionEngines = new EnumMap<>(ConversionType.class);
        for (ConversionEngine conversionEngine : conversionEngines) {
            ConversionEngine previous = this.conversionEngines.put(
                    conversionEngine.supportedType(),
                    conversionEngine
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "Bir dönüşüm tipi için birden fazla motor tanımlandı: "
                                + conversionEngine.supportedType()
                );
            }
        }
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
            ConversionEngine conversionEngine = conversionEngines.get(
                    conversionJob.getConversionType()
            );
            if (conversionEngine == null) {
                throw new DocumentConversionException(
                        "Dönüşüm motoru bulunamadı: " + conversionJob.getConversionType()
                );
            }

            String resultStorageKey = conversionEngine.convert(conversionJob);
            conversionJob.complete(resultStorageKey);
        } catch (RuntimeException exception) {
            LOGGER.error("Conversion failed for job {}", conversionJob.getId(), exception);
            conversionJob.fail(FAILURE_MESSAGE);
        }

        conversionJobRepository.save(conversionJob);
    }
}
