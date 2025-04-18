package com.yourname.backend.controllers;

import com.yourname.backend.entities.JobDescription;
import com.yourname.backend.entities.Resume;
import com.yourname.backend.repositories.JobDescriptionRepository;
import com.yourname.backend.repositories.ResumeRepository;
import com.yourname.backend.services.AiService;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/resume")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobRepo;
    private final StorageService storageService;
    private final AiService aiService;

    @Autowired
    public ResumeController(ResumeRepository resumeRepository,
                            JobDescriptionRepository jobRepo,
                            StorageService storageService,
                            AiService aiService) {
        this.resumeRepository = resumeRepository;
        this.jobRepo = jobRepo;
        this.storageService = storageService;
        this.aiService = aiService;
    }

    /**
     * Uploads a resume, scores it against the given job, and returns the saved Resume entity.
     *
     * @param file           the multipart file (PDF or DOCX)
     * @param candidateName  the name of the candidate
     * @param jobId          the ID of the JobDescription to score against
     */
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Resume upload(@RequestParam("file") @NotNull MultipartFile file,
                         @RequestParam("candidateName") @NotNull String candidateName,
                         @RequestParam("jobId") @NotNull Long jobId)
            throws IOException, InterruptedException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType) &&
                !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            throw new IllegalArgumentException("Only PDF or DOCX files are allowed");
        }

        // 1) Store resume on disk
        String resumePath = storageService.store(file);

        // 2) Create the Resume entity
        Resume resume = new Resume(
                file.getOriginalFilename(),
                candidateName,
                resumePath
        );
        resume.setContentType(contentType);
        resume.setSize(file.getSize());

        // 3) Lookup the job and dump its descriptionText to a temp .txt
        JobDescription job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid jobId: " + jobId));
        Path tempJob = Files.createTempFile("jobdesc-", ".txt");
        Files.writeString(tempJob, job.getDescriptionText());

        // 4) Shell out to Python, get a 0–100 score, persist it
        double score = aiService.scoreResume(resumePath, tempJob.toString());
        resume.setMatchScore(score);

        // 5) Save and return
        return resumeRepository.save(resume);
    }

    /**
     * Retrieves all stored resumes.
     */
    @GetMapping("/all")
    public List<Resume> list() {
        return resumeRepository.findAll();
    }
}
