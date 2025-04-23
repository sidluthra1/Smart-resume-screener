package com.yourname.backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.backend.dto.ResumeDto;
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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${python.resume-parser}")
    private String RESUME_PARSER;

    @Value("${python.job-parser}")
    private String JOB_PARSER;

    @Value("${python.semantic-matcher}")
    private String SEMANTIC_MATCHER;

    @Value("${python.text-extractor}")
    private String TEXT_EXTRACTOR;

    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    public ResumeController(ResumeRepository resumeRepo,
                            JobDescriptionRepository jobRepo,
                            StorageService storageService,
                            AiService aiService,
                            SkillService skillService) {
        this.resumeRepo = resumeRepo;
        this.jobRepo = jobRepo;
        this.storageService = storageService;
        this.aiService = aiService;
        this.skillService = skillService;
    }

    /* ----------------------------------------------------------
            POST /resume/upload
       ---------------------------------------------------------- */
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumeDto upload(@RequestParam("file") @NotNull MultipartFile file,
                            @RequestParam("candidateName") @NotNull String candidateName,
                            @RequestParam("jobId") @NotNull Long jobId) throws Exception {

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
                RESUME_PARSER,
                resumePath
        );

        ParsedResume parsedRes = mapper.readValue(parsedResumeJson, ParsedResume.class);

        String resumePlainTxt = runPythonCaptureText(
                TEXT_EXTRACTOR,      // path injected from application.properties
                resumePath
        );

        /* 3) prepare JobDescription text + run JobDescriptionParser.py ---------- */
        JobDescription job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid jobId " + jobId));

        Path tmpJobTxt = Files.createTempFile("job-", ".txt");
        Files.writeString(tmpJobTxt, job.getDescriptionText());

        String parsedJobJson = runPython(
                JOB_PARSER,
                tmpJobTxt.toString()
        );

        /* 4) get Overlap score --------------------------------------------------- */
        // 1) run the matcher
        String matcherJson = runPythonCaptureText(
                SEMANTIC_MATCHER,
                resumePath,
                tmpJobTxt.toString());

// 2) keep only the last line that looks like JSON
        String jsonLine = Arrays.stream(matcherJson.split("\\R"))
                .filter(l -> l.trim().startsWith("{") && l.trim().endsWith("}"))
                .reduce((a, b) -> b)             // take the last one
                .orElse("{}");


// 3) parse Overlap
        double overlapScore = 0.0;
        try {
            JsonNode node = JSON.readTree(matcherJson);
            overlapScore = node.get("Overlap").asDouble();
            log.debug("semantic_matcher Overlap = {}", overlapScore);
        } catch (Exception e) {
            log.warn("semantic_matcher output was not pure JSON → using Overlap = 0", e);
            overlapScore = 0.0;
        }
        Files.deleteIfExists(tmpJobTxt);
        String jobPlainTxt = job.getDescriptionText();

        /* 5) blended scoring ---------------------------------------------------- */
        AiService.ScoreBundle scores = aiService.scoreResume(
                resumePlainTxt, jobPlainTxt,
                parsedResumeJson, parsedJobJson,
                overlapScore);

        double finalScore = scores.finalScore();

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

        /* experience */
        Experience exp = new Experience();
        exp.setDescription(parsedRes.work_experience);
        exp.setResume(r);
        r.getExperiences().add(exp);

        Resume saved = resumeRepo.save(r);
        return toDto(saved, scores);
    }

    /* ----------------------------------------------------------
            GET /resume/all
       ---------------------------------------------------------- */
    @GetMapping("/all")
    public List<Resume> list() {
        return resumeRepo.findAll();
    }

    /* ===================================================================== */
    /* ======================  Helper methods  ============================= */
    /* ===================================================================== */

    /** run a python script; return *entire* stdout (stderr is merged) */
    private String runPython(String script, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);                 // merge stderr → stdout

        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        if (exit != 0) {
            log.error("{} failed (exit {}) ── output:\n{}", script, exit, out);
            throw new RuntimeException(script + " failed – see logs");
        }
        return out.trim();
    }

    /**
     * Executes the given script and returns only the *last* line printed to STDOUT.
     * We keep stderr merged into stdout to avoid dead‑locks, but ignore everything
     * except the final JSON line produced by the script.
     */
    private String runPythonCaptureText(String script, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);                 // keep merge – Option B

        Process p = pb.start();
        String last = null;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                last = line;
            }
        }
        if (last == null) last = "";

        int exit = p.waitFor();
        if (exit != 0) {
            log.error("{} failed (exit {}). Last line: {}", script, exit, last);
            throw new RuntimeException(script + " failed – see logs");
        }
        return last.trim();
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

    private ResumeDto toDto(Resume r, AiService.ScoreBundle scores) {
        Set<String> skillNames = r.getSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toSet());
        return new ResumeDto(
                r.getId(), r.getFileName(), r.getCandidateName(),
                scores.finalScore(),
                scores.semanticScore(),
                scores.skillsScore(),
                scores.educationScore(),
                scores.experienceScore(),
                scores.overlap(),
                scores.llmScore(),
                r.getEmail(), r.getPhone(),
                r.getSummary(), r.getEducation(),
                skillNames
        );
    }
}
