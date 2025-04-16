package com.yourname.backend.controllers;

import com.yourname.backend.entities.Resume;
import com.yourname.backend.repositories.ResumeRepository;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/resume")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final StorageService storageService;

    @Autowired
    public ResumeController(ResumeRepository resumeRepo, StorageService storageService) {
        this.resumeRepository = resumeRepo;
        this.storageService = storageService;
    }

    /**
     * Uploads a resume file (PDF or DOCX) and stores its metadata in the database.
     * @param file           the multipart file to upload
     * @param candidateName  the name of the candidate
     * @return the saved Resume entity
     * @throws IOException if file storage fails
     */
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Resume upload(@RequestPart("file") @NotNull MultipartFile file,
                         @RequestPart("candidateName") @NotNull String candidateName)
            throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType) &&
                !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            throw new IllegalArgumentException("Only PDF or DOCX files are allowed");
        }

        // Store the file on disk and get its path
        String path = storageService.store(file);

        // Create and save the Resume entity
        Resume resume = new Resume(
                file.getOriginalFilename(),
                candidateName,
                path
        );
        resume.setContentType(contentType);
        resume.setSize(file.getSize());

        return resumeRepository.save(resume);
    }

    /**
     * Retrieves all stored resumes.
     * @return list of Resume entities
     */
    @GetMapping("/all")
    public List<Resume> list() {
        return resumeRepository.findAll();
    }
}
