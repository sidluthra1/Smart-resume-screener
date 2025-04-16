package com.yourname.backend.controllers;

import com.yourname.backend.entities.Resume;
import com.yourname.backend.repositories.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeRepository resumeRepository;

    @PostMapping("/create")
    public Resume createResume(@RequestParam("fileName") String fileName,
                               @RequestParam("candidateName") String candidateName) {
        // For now, we are not actually uploading a file.
        // We'll do real file upload logic later.
        Resume resume = new Resume(fileName, candidateName, "/dummy/path/for/now");
        return resumeRepository.save(resume);
    }

    @GetMapping("/all")
    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }
}
