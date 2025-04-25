// src/main/java/com/yourname/backend/controllers/JobController.java
package com.yourname.backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.backend.dto.JobDescriptionDto;
import com.yourname.backend.entities.JobDescription;
import com.yourname.backend.entities.Skill;
import com.yourname.backend.repositories.JobDescriptionRepository;
import com.yourname.backend.services.SkillService;
import com.yourname.backend.storage.StorageService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.yourname.backend.util.CitationCleaner.strip;

@RestController
@RequestMapping("/job")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobDescriptionRepository jobRepo;
    private final StorageService           storageService;
    private final SkillService             skillService;
    private final ObjectMapper             JSON = new ObjectMapper();

    /* ---------- paths injected from application.properties ---------- */
    @Value("${python.text-extractor}")
    private String TEXT_EXTRACTOR;

    @Value("${python.job-parser}")
    private String JOB_PARSER;

    @Value("${ai.python-executable:python3}")
    private String PYTHON;

    @Autowired
    public JobController(JobDescriptionRepository jobRepo,
                         StorageService storageService,
                         SkillService skillService) {
        this.jobRepo        = jobRepo;
        this.storageService = storageService;
        this.skillService   = skillService;
    }

    /* ------------------------------------------------------------------
                              POST  /job/upload
       ------------------------------------------------------------------ */
    @PostMapping(path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public JobDescriptionDto upload(@RequestParam("file")  @NotNull MultipartFile file,
                                    @RequestParam("title") @NotNull String        title)
            throws Exception {

        /* 1) store the incoming file ---------------------------------- */
        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        String jdPath = storageService.store(file);

        /* 2) extract plain text from the file ------------------------- */
        String plainTxt = runPythonCaptureText(TEXT_EXTRACTOR, jdPath);

        /* 3) ask OpenAI to parse the JD ------------------------------- */
        // only the path – parser does its own text extraction if needed
        String parsedJson = runPython(JOB_PARSER, jdPath);
        JsonNode n = JSON.readTree(parsedJson);

        String summary = n.path("Job Description").asText(null);

        /* 4) skills → Set<Skill> -------------------------------------- */
        List<String> skillsList = toStringList(n.path("skills"));      // <-- USE helper
        Set<Skill> skills = skillService.fetchOrCreateSkills(
                skillsList.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toSet()));

// turn the list into Set<Skill> entities


        /* 5) requirements & responsibilities -------------------------- */
        List<String> reqList  = JSON.convertValue(
                n.path("Requirements").isMissingNode() ? List.of() : n.path("Requirements"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class));
        List<String> respList = JSON.convertValue(
                n.path("Responsibilities").isMissingNode() ? List.of() : n.path("Responsibilities"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class));

        String requirements     = String.join(", ", reqList);
        String responsibilities  = String.join(", ", respList);


        /* 6) build entity & persist ----------------------------------- */
        JobDescription jd = new JobDescription();
        jd.setTitle(title);
        jd.setCategory(n.path("Job Category").asText(null));
        jd.setLocation(n.path("Location").asText(null));
        jd.setDescriptionText(plainTxt);
        jd.setSummary(strip(summary));   // raw JD text
        jd.setSkills(skills);
        jd.setRequirements(strip(String.join(", ", reqList)));
        jd.setResponsibilities(strip(String.join(", ", respList)));
        jd.setParsedJson(parsedJson);

        JobDescription saved = jobRepo.save(jd);

        /* 7) return a clean DTO for the client ------------------------ */
        return toDto(saved, skillsList, reqList, respList);
    }

    /* ------------------------------------------------------------------
                              GET /job/all
       ------------------------------------------------------------------ */
    @GetMapping("/all")
    public List<JobDescriptionDto> list() {
        return jobRepo.findAll()
                .stream()
                .map(jd -> toDto(
                        jd,
                        jd.getSkills().stream().map(Skill::getName).toList(),
                        splitCsv(jd.getRequirements()),
                        splitCsv(jd.getResponsibilities())))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescription> getById(@PathVariable Long id) {
        return jobRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (!jobRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        jobRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ==================================================================
                            helper methods
       ================================================================== */

    /** Safely converts either an array node or a single comma-string to List<String> */
    private List<String> toStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull())
            return List.of();

        if (node.isArray()) {
            List<String> list = new ArrayList<>();
            node.forEach(jn -> list.add(jn.asText().trim()));
            return list.stream().filter(s -> !s.isBlank()).toList();
        }

        if (node.isTextual()) {
            return Arrays.stream(node.asText().split("\\s*,\\s*"))
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return List.of();
    }

    private List<String> splitCsv(String csv) {
        return (csv == null || csv.isBlank())
                ? List.of()
                : Arrays.stream(csv.split("\\s*,\\s*"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private JobDescriptionDto toDto(JobDescription jd,
                                    List<String> skills,
                                    List<String> req,
                                    List<String> resp) {
        return new JobDescriptionDto(
                jd.getId(),
                jd.getTitle(),
                jd.getCategory(),
                jd.getLocation(),
                jd.getSummary(),       // short description
                skills,                // list, easy for front-end
                req,
                resp,
                jd.getStatus()
        );
    }

    /* -------------- process helpers (same pattern as Resume side) ---- */
    private String runPython(String script, String... args)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        if (exit != 0) {
            log.error("{} failed (exit {}) — output:\n{}", script, exit, out);
            throw new RuntimeException(script + " failed – see logs");
        }
        return out.trim();
    }

    /** returns only the last line printed by the Python script */
    private String runPythonCaptureText(String script, String... args)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add(PYTHON);
        pb.command().add(script);
        pb.command().addAll(List.of(args));
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String last = null;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) last = line;
        }
        if (last == null) last = "";

        int exit = p.waitFor();
        if (exit != 0) {
            log.error("{} failed (exit {}). Last line: {}", script, exit, last);
            throw new RuntimeException(script + " failed – see logs");
        }
        return last.trim();
    }
}
