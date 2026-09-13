package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class CwebpImageConverter implements ConversionEngine {

    private final FileStorage fileStorage;
    private final String command;
    private final Duration timeout;

    public CwebpImageConverter(
            FileStorage fileStorage,
            @Value("${app.conversion.webp-command:cwebp}") String command,
            @Value("${app.conversion.webp-timeout:30s}") Duration timeout
    ) {
        this.fileStorage = fileStorage;
        this.command = command;
        this.timeout = timeout;
    }

    @Override
    public ConversionType supportedType() {
        return ConversionType.IMAGE_TO_WEBP;
    }

    @Override
    public String convert(ConversionJob conversionJob) {
        Path source = fileStorage.load(conversionJob.getSourceStorageKey());
        String resultStorageKey = replaceExtension(
                conversionJob.getSourceStorageKey(),
                ConversionType.IMAGE_TO_WEBP.getOutputExtension()
        );
        Path result = fileStorage.load(resultStorageKey);

        if (!Files.isRegularFile(source)) {
            throw new DocumentConversionException("Kaynak görsel bulunamadı");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                command,
                "-quiet",
                "-mt",
                "-metadata",
                "none",
                "-q",
                String.valueOf(conversionJob.getQuality()),
                source.toString(),
                "-o",
                result.toString()
        );
        processBuilder.redirectErrorStream(true);

        try {
            Files.deleteIfExists(result);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new DocumentConversionException("WebP dönüşümü zaman aşımına uğradı");
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (process.exitValue() != 0 || !Files.isRegularFile(result)) {
                throw new DocumentConversionException(
                        "WebP dönüşümü başarısız oldu" + formatOutput(output)
                );
            }

            return resultStorageKey;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DocumentConversionException("WebP dönüşümü kesildi", exception);
        } catch (IOException exception) {
            throw new DocumentConversionException("cwebp çalıştırılamadı", exception);
        }
    }

    private String replaceExtension(String storageKey, String extension) {
        int lastDot = storageKey.lastIndexOf('.');
        String baseName = lastDot < 0 ? storageKey : storageKey.substring(0, lastDot);
        return baseName + extension;
    }

    private String formatOutput(String output) {
        return output.isBlank() ? "" : ": " + output;
    }
}
