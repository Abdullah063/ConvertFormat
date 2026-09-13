package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;
import com.altun.convertformat.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ImageMagickConversionEngine implements ConversionEngine {

    private static final Set<ConversionType> SUPPORTED_TYPES = Set.of(
            ConversionType.WEBP_TO_JPEG,
            ConversionType.WEBP_TO_PNG,
            ConversionType.IMAGE_TO_PDF
    );

    private final FileStorage fileStorage;
    private final String command;
    private final Duration timeout;

    public ImageMagickConversionEngine(
            FileStorage fileStorage,
            @Value("${app.conversion.imagemagick-command:magick}") String command,
            @Value("${app.conversion.imagemagick-timeout:45s}") Duration timeout
    ) {
        this.fileStorage = fileStorage;
        this.command = command;
        this.timeout = timeout;
    }

    @Override
    public Set<ConversionType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public String convert(ConversionJob conversionJob) {
        Path source = fileStorage.load(conversionJob.getSourceStorageKey());
        String resultStorageKey = replaceExtension(
                conversionJob.getSourceStorageKey(),
                conversionJob.getConversionType().getOutputExtension()
        );
        Path result = fileStorage.load(resultStorageKey);

        if (!Files.isRegularFile(source)) {
            throw new DocumentConversionException("Kaynak görsel bulunamadı");
        }

        List<String> arguments = buildArguments(conversionJob, source, result);
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.redirectErrorStream(true);

        try {
            Files.deleteIfExists(result);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new DocumentConversionException("Görsel dönüşümü zaman aşımına uğradı");
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (process.exitValue() != 0 || !hasExpectedSignature(result, conversionJob.getConversionType())) {
                throw new DocumentConversionException(
                        "ImageMagick dönüşümü başarısız oldu" + formatOutput(output)
                );
            }

            return resultStorageKey;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DocumentConversionException("Görsel dönüşümü kesildi", exception);
        } catch (IOException exception) {
            throw new DocumentConversionException("ImageMagick çalıştırılamadı", exception);
        }
    }

    private List<String> buildArguments(
            ConversionJob conversionJob,
            Path source,
            Path result
    ) {
        List<String> arguments = new ArrayList<>(List.of(
                command,
                "-limit", "memory", "256MiB",
                "-limit", "map", "512MiB",
                source.toString(),
                "-auto-orient",
                "-strip"
        ));

        if (conversionJob.getConversionType() == ConversionType.WEBP_TO_JPEG) {
            arguments.add("-quality");
            arguments.add(String.valueOf(conversionJob.getQuality()));
        }

        if (conversionJob.getConversionType() == ConversionType.IMAGE_TO_PDF) {
            arguments.addAll(List.of("-background", "white", "-alpha", "remove", "-alpha", "off"));
        }

        arguments.add(outputArgument(conversionJob.getConversionType(), result));
        return arguments;
    }

    private String outputArgument(ConversionType conversionType, Path result) {
        return switch (conversionType) {
            case WEBP_TO_JPEG -> "jpeg:" + result;
            case WEBP_TO_PNG -> "png:" + result;
            case IMAGE_TO_PDF -> "pdf:" + result;
            default -> throw new DocumentConversionException(
                    "ImageMagick bu dönüşüm tipini desteklemiyor: " + conversionType
            );
        };
    }

    private boolean hasExpectedSignature(Path result, ConversionType conversionType)
            throws IOException {
        if (!Files.isRegularFile(result)) {
            return false;
        }

        byte[] signature;
        try (var inputStream = Files.newInputStream(result)) {
            signature = inputStream.readNBytes(8);
        }

        return switch (conversionType) {
            case WEBP_TO_JPEG -> startsWith(signature, new byte[]{(byte) 0xff, (byte) 0xd8});
            case WEBP_TO_PNG -> startsWith(
                    signature,
                    new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
            );
            case IMAGE_TO_PDF -> startsWith(signature, new byte[]{0x25, 0x50, 0x44, 0x46});
            default -> false;
        };
    }

    private boolean startsWith(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) {
            return false;
        }

        for (int index = 0; index < expected.length; index++) {
            if (actual[index] != expected[index]) {
                return false;
            }
        }
        return true;
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
