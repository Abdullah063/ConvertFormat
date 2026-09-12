package com.altun.convertformat.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorage {

    public abstract String store(MultipartFile file) throws IOException;

    public abstract Path load(String storageKey);

    public abstract void delete(String storageKey) throws IOException;
}