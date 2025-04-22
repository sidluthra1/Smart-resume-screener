package com.yourname.backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.backend.entities.*;
import com.yourname.backend.repositories.JobDescriptionRepository;
import com.yourname.backend.repositories.ResumeRepository;
import com.yourname.backend.services.AiService;
import com.yourname.backend.services.SkillService;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resume")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeRepository resumeRepo;
    private final JobDescriptionRepository jobRepo;
    private final StorageService storageService;
    private final AiService aiService;
    private final SkillService skillService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ResumeController(ResumeRepository resumeRepo,
                            JobDescriptionRepository jobRepo,
                            StorageService storageService,
                            AiService aiService,
                            SkillService skillService) {
        this.resumeRepo     = resumeRepo;
        this.jobRepo        = jobRepo;
        this.storageService = storageService;
        this.aiService      = aiService;
        this.skillService   = skillService;
    }

    /* ----------------------------------------------------------
            POST /resume/upload
       ---------------------------------------------------------- */
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Resume upload(@RequestParam("file")           @NotNull MultipartFile file,
                         @RequestParam("candidateName")  @NotNull String        candidateName,
                         @RequestParam("jobId")          @NotNull Long          jobId)
            throws Exception {

        /* 1) basic checks & store ------------------------------------------------ */
        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String ct = file.getContentType();
        if (!"application/pdf".equals(ct) &&
                !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(ct))
            throw new IllegalArgumentException("Only PDF or DOCX allowed");

        String resumePath = storageService.store(file);

        /* 2) run ResumeParser.py (structured JSON) ------------------------------ */
        String parsedResumeJson = runPython(
                "src/main/python/ResumeParser.py",
                resumePath
        );

        ParsedResume parsedRes = mapper.readValue(parsedResumeJson, ParsedResume.class);

        /* 3) prepare JobDescription text + run JobDescriptionParser.py ---------- */
        JobDescription job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid jobId " + jobId));

        Path tmpJobTxt = Files.createTempFile("job-", ".txt");
        Files.writeString(tmpJobTxt, job.getDescriptionText());

        String parsedJobJson = runPython(
                "src/main/python/JobDescriptionParser.py",
                tmpJobTxt.toString()
        );

        /* 4) get plain‑text of resume using semantic_matcher.py --text ---------- */
        String resumePlainTxt = runPythonCaptureText(
                "src/main/python/semantic_matcher.py",
                "--text",
                resumePath
        );

        String jobPlainTxt = job.getDescriptionText();

        /* 5) blended scoring ---------------------------------------------------- */
        double finalScore = aiService.scoreResume(
                resumePlainTxt,
                jobPlainTxt,
                parsedResumeJson,
                parsedJobJson
        );

        /* 6) build & persist Resume entity ------------------------------------- */
        Resume r = new Resume(file.getOriginalFilename(), candidateName, resumePath);
        r.setContentType(ct);
        r.setSize(file.getSize());
        r.setMatchScore(finalScore);

        r.setEmail(parsedRes.email);
        r.setPhone(parsedRes.phone_number);
        r.setSummary(parsedRes.summary);
        r.setEducation(parsedRes.education);

        /* skills */
        Set<String> skillNames = Arrays.stream(parsedRes.skills.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
        Set<Skill> skills = skillService.fetchOrCreateSkills(skillNames);
        r.setSkills(skills);

        /* experience (single string → one entity – expand later if you want) */
        Experience exp = new Experience();
        exp.setDescription(parsedRes.work_experience);
        exp.setResume(r);
        r.getExperiences().add(exp);

        return resumeRepo.save(r);
    }

    /* ----------------------------------------------------------
            GET /resume/all
       ---------------------------------------------------------- */
    @GetMapping("/all")
    public List<Resume> list() {
        return resumeRepo.findAll();
    }

    /* ========================================================================== */
    /* =========================  Helper methods  ============================== */
    /* ========================================================================== */

    /** run an arbitrary python script and return **stdout** as String */
    private String runPython(String script, String... args)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add("python3");            // or inject from properties
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit  = p.waitFor();
        if (exit != 0) {
            log.error("{} failed (exit {}) ── output:\n{}", script, exit, out);
            throw new RuntimeException(script + " failed – see logs");
        }
        return out.trim();
    }

    /** same as above but used when script prints ONLY text (no JSON) */
    private String runPythonCaptureText(String script, String... args)
            throws IOException, InterruptedException {
        return runPython(script, args);  // identical right now – separate for clarity
    }

    /* DTO mirroring ResumeParser.py schema */
    private static class ParsedResume {
        public String email;
        public String phone_number;
        public String summary;
        public String skills;
        public String work_experience;
        public String education;
    }
}
