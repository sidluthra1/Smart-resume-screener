package com.yourname.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path uploadRoot;

    // Constructor injection for the upload directory path from application properties
    public StorageService(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        // Create the directory if it doesn't exist
        Files.createDirectories(this.uploadRoot);
    }

    /**
     * Stores a file on disk using a generated unique name and returns the absolute path.
     *
     * @param file The MultipartFile received from the request.
     * @return The absolute path string of the stored file.
     * @throws IOException If an error occurs during file operations.
     * @throws IllegalArgumentException If the filename is invalid.
     */
    public String store(MultipartFile file) throws IOException {
        // Clean the path and get the original filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Basic validation for filename
        if (originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is empty");
        }
        // Prevent directory traversal vulnerability
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Extract the file extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex); // Includes the dot, e.g., ".pdf"
        }

        // Generate a unique filename using UUID to avoid collisions
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Resolve the destination path against the root upload directory
        Path destinationPath = this.uploadRoot.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
        return destinationPath.toAbsolutePath().toString();
    }

    public String storeText(String text, String prefix, String suffix) throws IOException {
        // generate a unique filename
        String filename = prefix + UUID.randomUUID() + suffix;
        Path dest = uploadRoot.resolve(filename);
        // write the text
        Files.writeString(dest, text, StandardCharsets.UTF_8);
        return dest.toAbsolutePath().toString();
    }
}