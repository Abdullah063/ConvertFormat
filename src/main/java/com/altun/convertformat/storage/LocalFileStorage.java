package com.altun.convertformat.storage;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorage {

    private final Path rootDirectory;

    public LocalFileStorage(
            @Value("${app.storage.directory:storage}") String directory
    ) throws IOException {
        this.rootDirectory = Path.of(directory)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(rootDirectory);
    }

    @Override
    public String store(MultipartFile file, String extension) throws IOException {
        String storageKey = UUID.randomUUID() + extension;
        Path target = resolveSafely(storageKey);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(
                    inputStream,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        return storageKey;
    }

    @Override
    public Path load(String storageKey) {
        return resolveSafely(storageKey);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafely(storageKey));
    }

    private Path resolveSafely(String storageKey) {
        Path resolved = rootDirectory.resolve(storageKey).normalize();

        if (!resolved.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Geçersiz dosya anahtarı");
        }

        return resolved;
    }
}
