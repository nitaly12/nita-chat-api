package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.chat.config.FileStorageConfig;
import org.example.chat.dto.reponse.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FileStorageConfig storageConfig;

    public UploadResponse store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1 && dot < original.length() - 1) {
            ext = "." + original.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        }

        String day = LocalDate.now().format(DAY);
        String filename = UUID.randomUUID() + ext;
        Path dir = storageConfig.getResolvedRoot().resolve(day);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);

        Path normalized = target.normalize();
        if (!normalized.startsWith(storageConfig.getResolvedRoot())) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try (var in = file.getInputStream()) {
            Files.copy(in, normalized, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = "/uploads/" + day + "/" + filename;
        return UploadResponse.builder()
                .url(url)
                .type(file.getContentType())
                .name(original)
                .size(file.getSize())
                .build();
    }
}
