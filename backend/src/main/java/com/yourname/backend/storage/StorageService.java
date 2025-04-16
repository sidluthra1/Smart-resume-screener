package com.yourname.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path uploadRoot;

    public StorageService(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.uploadRoot = Path.of(uploadDir);
        Files.createDirectories(this.uploadRoot);
    }

    /**
     * Stores a file on disk and returns the absolute path.
     */
    public String store(MultipartFile file) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = original.substring(original.lastIndexOf('.') + 1);
        String filename  = UUID.randomUUID() + "." + extension;              // unique
        Path dest = uploadRoot.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString();
    }
}
