package com.yourname.backend.controllers;

import com.yourname.backend.entities.JobDescription;
import com.yourname.backend.repositories.JobDescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job")
public class JobController {

    @Autowired
    private JobDescriptionRepository jobRepo;

    @PostMapping("/create")
    public JobDescription createJob(@RequestParam("title") String title,
                                    @RequestParam("descriptionText") String descriptionText) {
        JobDescription jd = new JobDescription(title, descriptionText);
        return jobRepo.save(jd);
    }

    @GetMapping("/all")
    public List<JobDescription> getAllJobs() {
        return jobRepo.findAll();
    }
}