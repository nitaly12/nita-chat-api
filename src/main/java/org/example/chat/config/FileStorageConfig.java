package org.example.chat.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Value("${chat.upload.dir:./uploads}")
    private String uploadDir;

    private Path resolvedRoot;

    @PostConstruct
    void init() throws IOException {
        resolvedRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(resolvedRoot);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(root.toUri().toString());
    }
}
