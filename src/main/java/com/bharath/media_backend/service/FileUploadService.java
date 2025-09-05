package com.bharath.media_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
public class FileUploadService {

    private final Path uploadDir;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /** Store uploaded file */
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file not allowed");
        }

        String filename = System.currentTimeMillis() + "-" + StringUtils.cleanPath(file.getOriginalFilename());
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/files/" + filename;
    }

    /** Load file as Spring Resource for streaming or download */
    public Resource loadFileAsResource(String filename) throws MalformedURLException {
        Path filePath = uploadDir.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filename);
        }
    }
}
