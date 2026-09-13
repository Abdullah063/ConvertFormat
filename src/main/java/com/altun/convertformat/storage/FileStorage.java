package com.altun.convertformat.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorage {

    String store(MultipartFile file, String extension) throws IOException;

    Path load(String storageKey);

    void delete(String storageKey) throws IOException;
}
