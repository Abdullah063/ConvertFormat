package com.altun.convertformat.conversion;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Component
public class DocxFileValidator {

    static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Dosya boş olamaz");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya en fazla 10 MB olabilir");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new IllegalArgumentException("Yalnızca DOCX dosyaları kabul edilir");
        }
    }
}
