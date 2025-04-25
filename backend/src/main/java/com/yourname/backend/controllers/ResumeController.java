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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResumeDto> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String newStatus
    ) {
        Resume r = resumeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        r.setStatus(newStatus);
        Resume saved = resumeRepo.save(r);
        // return updated DTO
        return ResponseEntity.ok(toDto(saved, null));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        Resume r = resumeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Path file = Paths.get(r.getFilePath());
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + r.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(r.getContentType()))
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resume> getById(@PathVariable Long id) {
        return resumeRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (!resumeRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        resumeRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumeDto upload(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("candidateName") @NotNull String candidateName,
            @RequestParam(value = "jobId", required = false) Long jobId
    ) throws Exception {

        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String ct = file.getContentType();
        if (!"application/pdf".equals(ct) &&
                !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(ct))
            throw new IllegalArgumentException("Only PDF or DOCX allowed");

        String resumePath = storageService.store(file);

        // parse resume JSON + text
        String parsedResumeJson = runPython(RESUME_PARSER, resumePath);
        ParsedResume parsedRes = mapper.readValue(parsedResumeJson, ParsedResume.class);
        String resumePlainTxt = runPythonCaptureText(TEXT_EXTRACTOR, resumePath);

        AiService.ScoreBundle scores = null;
        double finalScore = 0.0;

        if (jobId != null) {
            // fetch job and parse
            JobDescription job = jobRepo.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid jobId " + jobId));

            Path tmp = Files.createTempFile("job-", ".txt");
            Files.writeString(tmp, job.getDescriptionText());
            String parsedJobJson = runPython(JOB_PARSER, tmp.toString());

            // semantic matcher
            String matcherJson = runPythonCaptureText(SEMANTIC_MATCHER, resumePath, tmp.toString());
            double overlap = parseOverlap(matcherJson);
            Files.deleteIfExists(tmp);

            // score resume
            scores = aiService.scoreResume(
                    resumePlainTxt,
                    job.getDescriptionText(),
                    parsedResumeJson,
                    parsedJobJson,
                    overlap
            );
            finalScore = scores.finalScore();
        }

        // build resume entity
        Resume r = new Resume(file.getOriginalFilename(), candidateName, resumePath);
        r.setContentType(ct);
        r.setSize(file.getSize());
        r.setMatchScore(finalScore);
        r.setEmail(parsedRes.email);
        r.setPhone(parsedRes.phone_number);
        r.setSummary(parsedRes.summary);
        r.setEducation(parsedRes.education);
        r.setStatus("New");

        Set<String> skillNames = Arrays.stream(parsedRes.skills.split(","))
                .map(String::trim)
                .filter(n -> !n.isBlank())
                .collect(Collectors.toSet());
        Set<Skill> skills = skillService.fetchOrCreateSkills(skillNames);
        r.setSkills(skills);

        Experience exp = new Experience();
        exp.setDescription(parsedRes.work_experience);
        exp.setResume(r);
        r.getExperiences().add(exp);

        Resume saved = resumeRepo.save(r);
        return toDto(saved, scores);
    }

    @GetMapping("/all")
    public List<Resume> list() {
        return resumeRepo.findAll();
    }

    private double parseOverlap(String json) {
        try {
            JsonNode n = JSON.readTree(json);
            return n.has("Overlap") ? n.get("Overlap").asDouble() : 0.0;
        } catch (Exception e) {
            log.warn("Could not parse overlap", e);
            return 0.0;
        }
    }

    private String runPython(String script, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        if (exit != 0) throw new RuntimeException(script + " failed: " + out);
        return out.trim();
    }

    private String runPythonCaptureText(String script, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String line, last = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while ((line = r.readLine()) != null) last = line;
        }
        if (p.waitFor() != 0) throw new RuntimeException(script + " failed");
        return last == null ? "" : last.trim();
    }

    private ResumeDto toDto(Resume r, AiService.ScoreBundle scores) {
        Set<String> names = r.getSkills().stream()
                .map(Skill::getName)
                .collect(Collectors.toSet());
        return new ResumeDto(
                r.getId(), r.getFileName(), r.getCandidateName(),
                scores == null ? 0.0 : scores.finalScore(),
                scores == null ? 0.0 : scores.semanticScore(),
                scores == null ? 0.0 : scores.skillsScore(),
                scores == null ? 0.0 : scores.educationScore(),
                scores == null ? 0.0 : scores.experienceScore(),
                scores == null ? 0.0 : scores.overlap(),
                scores == null ? 0.0 : scores.llmScore(),
                r.getEmail(), r.getPhone(), r.getSummary(), r.getEducation(),
                names, r.getStatus()
        );
    }

    private static class ParsedResume {
        public String email;
        public String phone_number;
        public String summary;
        public String skills;
        public String work_experience;
        public String education;
    }
}
