package com.altun.convertformat.conversion;

import com.altun.convertformat.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class LibreOfficeDocumentConverter implements DocumentConverter {

    private final FileStorage fileStorage;
    private final String command;
    private final Duration timeout;

    public LibreOfficeDocumentConverter(
            FileStorage fileStorage,
            @Value("${app.conversion.command:libreoffice}") String command,
            @Value("${app.conversion.timeout:60s}") Duration timeout
    ) {
        this.fileStorage = fileStorage;
        this.command = command;
        this.timeout = timeout;
    }

    @Override
    public String convertToPdf(String sourceStorageKey) {
        Path source = fileStorage.load(sourceStorageKey);
        String resultStorageKey = replaceExtension(sourceStorageKey, ".pdf");
        Path result = fileStorage.load(resultStorageKey);

        ensureSourceExists(source);

        ProcessBuilder processBuilder = new ProcessBuilder(
                command,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                source.getParent().toString(),
                source.toString()
        );
        processBuilder.redirectErrorStream(true);

        try {
            Files.deleteIfExists(result);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new DocumentConversionException("Dönüşüm zaman aşımına uğradı");
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (process.exitValue() != 0 || !Files.isRegularFile(result)) {
                throw new DocumentConversionException(
                        "LibreOffice dönüşümü başarısız oldu" + formatOutput(output)
                );
            }

            return resultStorageKey;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DocumentConversionException("Dönüşüm işlemi kesildi", exception);
        } catch (IOException exception) {
            throw new DocumentConversionException("LibreOffice çalıştırılamadı", exception);
        }
    }

    private void ensureSourceExists(Path source) {
        if (!Files.isRegularFile(source)) {
            throw new DocumentConversionException("Kaynak dosya bulunamadı");
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
