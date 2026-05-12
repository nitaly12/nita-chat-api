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
        return storeUnder(file, LocalDate.now().format(DAY));
    }

    public UploadResponse storeUnder(MultipartFile file, String subdir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String safeSub = subdir == null ? "" : subdir.replaceAll("[^A-Za-z0-9/_-]", "");
        if (safeSub.isBlank()) {
            throw new IllegalArgumentException("subdir must not be blank");
        }

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1 && dot < original.length() - 1) {
            ext = "." + original.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        }

        String filename = UUID.randomUUID() + ext;
        Path dir = storageConfig.getResolvedRoot().resolve(safeSub);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename).normalize();

        if (!target.startsWith(storageConfig.getResolvedRoot())) {
            throw new IllegalArgumentException("Invalid upload path");
        }

        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = "/uploads/" + safeSub + "/" + filename;
        return UploadResponse.builder()
                .url(url)
                .type(file.getContentType())
                .name(original)
                .size(file.getSize())
                .build();
    }
}
